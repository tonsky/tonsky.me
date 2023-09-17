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
    [site.core :as core]
    [site.pages.atom :as atom]
    [site.pages.default :as default]
    [site.pages.index :as index]
    [site.pages.post :as post]
    [site.parser :as parser]
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
       :headers {"Content-Type" "text/plain; charset=UTF-8"}
       :body    (str "Path '" (:uri req) "' not found")})
    (wrap-files "_site")
    (wrap-files "site")
    (router/wrap-routes
      (router/routes
        "GET /" []
        {:status  200
         :headers {"Content-Type" "text/html; charset=UTF-8"}
         :body    (-> (index/index)
                    default/default
                    :content
                    render/render-html)}
        
        "GET /blog/atom.xml" []
        {:status  200
         :headers {"Content-Type" "application/atom+xml; charset=UTF-8"}
         :body    (render/render-xml (atom/feed))}
        
        "GET /blog/*" [id]
        (let [dir  (io/file (str "site/blog/" id))
              file (io/file dir "index.md")]
          (when (.exists file)
            {:status  200
             :headers {"Content-Type" "text/html; charset=UTF-8"}
             :body    (-> (parser/parse-md file)
                        post/post
                        default/default
                        :content
                        render/render-html)}))
        
        "GET /about" []
        {:status 301
         :headers {"Location" "/projects/"}}))
    wrap-decode-uri
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
