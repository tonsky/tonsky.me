(ns site.server
  (:require
    [clj-simple-router.core :as router]
    [clojure.java.io :as io]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [ring.middleware.cookies :as ring-cookies]
    [ring.middleware.head :as ring-head]
    [ring.middleware.params :as ring-params]
    [ring.util.codec :as ring-codec]
    [ring.util.io :as ring-io]
    [ring.util.mime-type :as ring-mime]
    [ring.util.time :as ring-time]
    [site.cache :as cache]
    [site.core :as core]
    [site.pages.atom :as atom]
    [site.pages.default :as default]
    [site.pages.design :as design]
    [site.pages.index :as index]
    [site.pages.patrons :as patrons]
    [site.pages.post :as post]
    [site.pages.projects :as projects]
    [site.pages.talks :as talks]
    [site.parser :as parser]
    [site.pointers :as pointers]
    [site.watcher :as watcher]
    [site.render :as render])
  (:import
    [java.io File]))

(defn find-file ^File [path]
  (let [file ^File (io/file path)]
    (when (.exists file)
      (let [file (if (.isDirectory file)
                   (io/file file "index.html")
                   file)]
        (when (.exists file)
          file)))))

(defn wrap-range [handler]
  (fn [req]
    (let [resp  (handler req)
          range (-> req :headers (get "range")) ;; Range: bytes=97555-
          body  (:body resp)]
      (if (and range (re-matches #"bytes=\d+-" range) (instance? File body))
        (with-open [is (java.io.FileInputStream. ^File body)]
          (let [chunk (* 1024 1024)
                start (-> (re-matches #"bytes=(\d+)-" range) second parse-long)
                end   (.length ^File body)
                len   (min chunk (- end start))
                bytes (byte-array len)]
            (.skip is start)
            (.readNBytes is bytes 0 len)
            (-> resp
              (assoc :status 206)
              (assoc :body (java.io.ByteArrayInputStream. bytes))
              (update :headers assoc "Accept-Ranges" "bytes")
              (update :headers assoc "Content-Length" len)
              (update :headers assoc "Content-Range" (str "bytes " start "-" (+ start len) "/" end)))))
        resp))))

(defn wrap-files [handler dir]
  (fn [req]
    (if-some [file (find-file (str dir "/" (:uri req)))]
      {:status 200
       :body   file
       :headers {"Content-Length" (.length file)
                 "Last-Modified"  (ring-time/format-date (ring-io/last-modified-date file))
                 "Content-Type"   (ring-mime/ext-mime-type (.getName file))}}
      (handler req))))

(defn wrap-decode-uri [handler]
  (fn [req]
    (handler (update req :uri ring-codec/url-decode))))

; (def *cache
;   (atom {}))

; (defn midnight-milli []
;   (->
;     (java.time.LocalDate/now core/UTC)
;     (.atStartOfDay core/UTC)
;     (.getLong java.time.temporal.ChronoField/INSTANT_SECONDS)
;     (* 1000)))

; (defn cached-by-files [key req resp files body-fn]
;   (let [modified  (transduce (map #(.lastModified ^File %)) max 0 files)
;         modified  (max modified (midnight-milli))
;         formatted (-> modified
;                     (Instant/ofEpochMilli)
;                     (core/format-temporal DateTimeFormatter/RFC_1123_DATE_TIME))
;         etag      (str "W/\"" (-> modified (Long/toString 16)) "\"")
;         resp      (merge-with merge
;                     {:status  200
;                      :headers {"Last-Modified" formatted
;                                "ETag"          etag
;                                "Cache-Control" "no-cache"}}
;                     resp)]
;     (if (and (not core/dev?) (#'ring-modified/cached-response? req resp))
;       (assoc resp
;         :status  304
;         :headers {"Content-Length" 0}
;         :body    nil)
;       (let [[ts val] (@*cache key)]
;         (if (= ts modified)
;           (assoc resp :body val)
;           (let [val (body-fn)]
;             (swap! *cache assoc key [modified val])
;             (assoc resp :body val)))))))

(defn resp-html
  ([page]
   (resp-html nil page))
  ([req page]
   {:status  200
    :headers {"Content-Type" "text/html; charset=UTF-8"}
    :body    (-> page
               default/default
               cache/timestamp
               :content
               render/render-html)}))

(defn redirect [loc]
  {:status 301
   :headers {"Location" loc}})

(defn wrap-redirects [handler]
  (fn [{:keys [uri] :as req}]
    (if (re-matches #"(/blog/[^/]+|/talks|/projects|/design|/patrons|/subscribe)" uri)
      (redirect (str uri "/"))
      (handler req))))

(def router
  (->
    (router/router
      (merge
        pointers/routes
        (router/routes
          "GET /"          req  (resp-html req (index/index))
          "GET /atom.xml"  []   {:status  200
                                 :headers {"Content-Type" "application/atom+xml; charset=UTF-8"}
                                 :body    (render/render-xml (atom/feed))}
          "GET /blog/**"   req  (let [[id] (:path-params req)]
                                  (when (.exists (io/file (str "site/blog/" id "/index.md")))
                                    (resp-html req (post/post (parser/parse-md (str "/blog/" id))))))
          "GET /talks"     req  (resp-html req (talks/page))
          "GET /design"    req  (resp-html req (design/page))
          "GET /patrons"   req  (resp-html req (patrons/page))
          "GET /projects"  req  (resp-html req (projects/page))
          "GET /subscribe" req  (-> (parser/parse-md "/subscribe/")
                                  post/post
                                  (dissoc :categories)
                                  (->> (resp-html req)))
          "GET /blog/how-to-subscribe/" [] (redirect "/subscribe/")
          "GET /blog/atom.xml"          [] (redirect "/atom.xml")
          "GET /about"                  [] (redirect "/projects/")
          )))
    cache/wrap-cached))

(def app
  (->
    (fn [req]
      {:status  404
       :headers {"Content-Type" "text/plain; charset=UTF-8"}
       :body    (str "Path '" (:uri req) "' not found")})
    (wrap-files "files")
    (wrap-files "site")
    wrap-range
    ((fn [handler]
       (fn [req]
         (or (router req) (handler req)))))
    wrap-redirects
    watcher/wrap-watcher
    wrap-decode-uri
    ring-cookies/wrap-cookies
    ring-params/wrap-params
    ring-head/wrap-head))

(mount/defstate server
  :start
  (let [opts   {:legacy-return-value? false
                :ip   core/server-ip
                :port core/server-port}
        server (http/run-server app opts)]
    (println "Started HTTP server on" (str (:ip opts) ":" (:port opts)))
    server)
  :stop
  (do
    (http/server-stop! server)
    (println "Stopped HTTP server")))

(defn -main [& args]
  (core/apply-args args)
  (mount/start))

(defn before-ns-unload []
  (mount/stop #'server))
