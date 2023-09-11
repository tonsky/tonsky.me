(ns site.server
  (:require
    [clj-simple-router.core :as router]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [ring.middleware.content-type :as content-type]
    [ring.util.response :as response]
    [site.parser :as parser]))

(def app
  (->
    (router/router
      (router/routes
        "GET /**" [path]
        (response/file-response (str "_site/" path))
        
        "GET /blog/*" [id]
        {:status  200
         :headers {"Content-type" "text/html;charset=UTF-8"}
         :body    (-> (str "blog/" id "/index.md")
                    slurp
                    parser/parse
                    parser/transform
                    parser/page
                    parser/render)}))
    content-type/wrap-content-type))

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
