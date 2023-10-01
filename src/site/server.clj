(ns site.server
  (:require
    [clj-simple-router.core :as router]
    [clojure.java.io :as io]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [ring.middleware.head :as ring-head]
    [ring.middleware.not-modified :as ring-modified]
    [ring.middleware.params :as ring-params]
    [ring.util.codec :as ring-codec]
    [ring.util.io :as ring-io]
    [ring.util.mime-type :as ring-mime]
    [ring.util.time :as ring-time]
    [site.core :as core]
    [site.pages.atom :as atom]
    [site.pages.default :as default]
    [site.pages.index :as index]
    [site.pages.post :as post]
    [site.parser :as parser]
    [site.pointers :as pointers]
    [site.render :as render])
  (:import
    [java.io File]
    [java.time Instant]
    [java.time.format DateTimeFormatter]))

(defn find-file ^File [path]
  (let [file ^File (io/file path)]
    (when (.exists file)
      (let [file (if (.isDirectory file)
                   (io/file file "index.html")
                   file)]
        (when (.exists file)
          file)))))

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

(def *cache
  (atom {}))

(defn cached-by-files [key req resp files body-fn]
  (let [modified  (transduce (map #(.lastModified ^File %)) max 0 files)
        formatted (-> modified
                    (Instant/ofEpochMilli)
                    (core/format-temporal DateTimeFormatter/RFC_1123_DATE_TIME))
        etag     (str "W/\"" (-> modified (Long/toString 16)) "\"")
        resp     (merge-with merge
                   {:status  200
                    :headers {"Last-Modified" formatted
                              "ETag"          etag
                              "Cache-Control" "no-cache"}}
                   resp)]
    (if (and (not core/dev?) (#'ring-modified/cached-response? req resp))
      (assoc resp
        :status 304
        :headers {"Content-Length" 0}
        :body nil)
      (let [[ts val] (@*cache key)]
        (if (= ts modified)
          (assoc resp :body val)
          (let [val (body-fn)]
            (swap! *cache assoc key [modified val])
            (assoc resp :body val)))))))

(defn post [req]
  (let [[id] (:path-params req)
        dir  (io/file (str "site/blog/" id))
        file (io/file dir "index.md")]
    (when (.exists file)
      (cached-by-files id req {:headers {"Content-Type" "text/html; charset=UTF-8"}}
        (file-seq dir)
        #(-> (parser/parse-md file)
           post/post
           default/default
           :content
           render/render-html)))))

(def app
  (->
    (fn [req]
      {:status  404
       :headers {"Content-Type" "text/plain; charset=UTF-8"}
       :body    (str "Path '" (:uri req) "' not found")})
    (wrap-files "_site")
    (wrap-files "site")
    (router/wrap-routes
      (merge
        pointers/routes
        (router/routes
          "GET /" []
          (cached-by-files "/" req {:headers {"Content-Type" "text/html; charset=UTF-8"}}
            (file-seq (io/file "site"))
            #(-> (index/index)
               default/default
               :content
               render/render-html))
          
          "GET /blog/atom.xml" []
          (cached-by-files "atom" req {:headers {"Content-Type" "application/atom+xml; charset=UTF-8"}}
            (file-seq (io/file "site"))
            #(render/render-xml (atom/feed)))
          
          "GET /blog/*" req
          (post req)
          
          "GET /about" []
          {:status 301
           :headers {"Location" "/projects/"}})))
    wrap-decode-uri
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
