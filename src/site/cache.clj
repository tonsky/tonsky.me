(ns site.cache
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure+.core :refer [cond+]]
   [ring.middleware.not-modified :as ring-modified]
   [site.core :as core])
  (:import
   [java.io File]
   [java.time Instant]
   [java.time.format DateTimeFormatter]))

(def ^:dynamic *touched*
  nil)

(defn touched ^File [file]
  (when *touched*
    (vswap! *touched* conj file))
  file)

(defn find-file [page uri]
  (when-some [uri' (cond
                     (str/starts-with? uri "https://tonsky.me/") (subs uri 17)
                     (str/starts-with? uri "http")               nil
                     (str/starts-with? uri "/")                  uri
                     :else                                       (str (:uri page) "/" uri))]
    (let [file  (io/file (str "site/" uri'))
          file' (if (.exists file) file (io/file (str "files/" uri')))]
      (when (.exists file')
        (touched file')
        file'))))

(defn timestamp-form [page form]
  (or
    (when-some [[tag attrs content] (core/normalize-tag form)]
      (cond
        (= :img tag)
        (when-some [file (find-file page (:src attrs))]
          (let [src'   (core/timestamp-url (:src attrs) file)
                [w h]  (core/image-dimensions file)
                style' (str (str "aspect-ratio: " w "/" h "; ") (:style attrs))]
            (core/consv :img (assoc attrs :src src' :style style' :width w :height h) content)))
        
        (= :video tag)
        (let [[_ source-attrs _] (core/normalize-tag (first content))]
          (when-some [file (find-file page (:src source-attrs))]
            (let [src'  (core/timestamp-url (:src source-attrs) file)
                  [w h] (core/video-dimensions file)]
              [:video (assoc attrs :width w :height h)
               [:source (assoc source-attrs :src src')]])))
        
        (and (= :link tag) (= "stylesheet" (:rel attrs)))
        (when-some [file (find-file page (:href attrs))]
          (core/consv :link (update attrs :href core/timestamp-url file) content))

        (and
          (= :meta tag)
          (or
            (= "og:image" (:property attrs))
            (= "twitter:image" (:name attrs))))
        (when-some [file (find-file page (:content attrs))]
          (core/consv :meta (update attrs :content core/timestamp-url file) content))
        
        (and (= :script tag) (:src attrs))
        (when-some [file (find-file page (:src attrs))]
          (core/consv :script (update attrs :src core/timestamp-url file) content))
        
        (:background-image attrs)
        (when-some [file (find-file page (:background-image attrs))]
          (let [url (core/timestamp-url (:background-image attrs) file)
                style (str "background-image: url('" url "'); ")
                attrs (-> attrs
                        (dissoc :background-image)
                        (update :style str style))]
            (core/consv tag attrs content)))
        
        (and (= :a tag) (:href attrs))
        (when (and (str/starts-with? (:href attrs) "http")
                (not (str/starts-with? (:href attrs) "https://tonsky.me")))
          (core/consv tag (assoc attrs :target "_blank") content))
        
        :else
        nil))    
    form))

(defn timestamp [page]
  (update page :content
    (fn [form]
      (walk/postwalk #(timestamp-form page %) form))))

(defn midnight-milli []
  (->
    (java.time.LocalDate/now core/UTC)
    (.atStartOfDay core/UTC)
    (.getLong java.time.temporal.ChronoField/INSTANT_SECONDS)
    (* 1000)))

(defn wrap-cached-impl [handler]
  (let [*cache (volatile! {})]
    (fn [req]
      (cond 
        (str/starts-with? (:uri req) "/ptrs")
        (handler req)
        
        (str/starts-with? (:uri req) "/watcher")
        (handler req)
        
        :else
        (binding [*touched* (volatile! #{})]
          (let [key (:uri req)
                {:keys [last-modified files resp]} (@*cache key)
                modified (transduce (map #(.lastModified ^File %)) max 0 files)
                modified (max modified (midnight-milli))]
            (cond+
              ;; invalidate cache
              (or (nil? resp) (> modified last-modified))
              (when-some [resp (handler req)]
                (let [formatted (-> modified
                                  (Instant/ofEpochMilli)
                                  (core/format-temporal DateTimeFormatter/RFC_1123_DATE_TIME))
                      etag      (str "W/\"" (-> modified (Long/toString 16)) "\"")
                      headers   {"Last-Modified" formatted
                                 "ETag"          etag
                                 "Cache-Control" "no-cache, max-age=315360000"}
                      resp      (update resp :headers merge headers)]
                  (vswap! *cache assoc key
                    {:last-modified modified
                     :files         @*touched*
                     :resp          resp})
                  resp))
            
              ;; serve Not-Modified
              (#'ring-modified/cached-response? req resp)
              (assoc resp
                :status  304
                :headers {"Content-Length" 0}
                :body    nil)
            
              ;; serve cached
              :else
              resp)))))))

(defn wrap-cached [handler]
  (if core/dev?
    handler
    (wrap-cached-impl handler)))
