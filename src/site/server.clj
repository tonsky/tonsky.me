(ns site.server
  (:require
    [clj-simple-router.core :as router]
    [clojure.java.io :as io]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [ring.middleware.head :as ring-head]
    [ring.util.codec :as ring-codec]
    [ring.util.io :as ring-io]
    [ring.util.mime-type :as ring-mime]
    [ring.util.time :as ring-time]
    [site.parser :as parser]
    [site.render :as render]
    [site.templates :as templates])
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

(def app
  (->
    (fn [req]
      {:status  404
       :headers {"Content-type" "text/plain; charset=UTF-8"}
       :body    (str "Path '" (:uri req) "' not found")})
    (wrap-files "_site")
    (wrap-files "site")
    (router/wrap-routes
      (router/routes
        "GET /blog/*" [id]
        (let [file (io/file (str "site/blog/" id "/index.md"))]
          (when (.exists file)
            {:status  200
             :headers {"Content-type" "text/html; charset=UTF-8"}
             :body    (-> (slurp file)
                        parser/parse
                        parser/transform
                        (assoc 
                          :url (str "/blog/" id "/")
                          :categories #{:blog})
                        templates/post
                        templates/default
                        :content
                        render/render)}))
        
        "GET /about" []
        {:status 301
         :headers {"Location" "/projects/"}}))
    wrap-decode-uri
    ring-head/wrap-head))

(def *server-opts
  (atom
    {:legacy-return-value? false
     :port 8080}))

(mount/defstate server
  :start
  (let [opts   @*server-opts
        server (http/run-server app opts)]
    (println "Started HTTP server on port" (:port opts))
    server)
  :stop
  (do
    (http/server-stop! server)
    (println "Stopped HTTP server")))

(defn -main [& {:as args}]
  (when-some [port (args "--port")]
    (swap! *server-opts assoc :port (parse-long port)))
  (mount/start))
