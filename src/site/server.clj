(ns site.server
  (:require
    [clj-simple-router.core :as router]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [ring.util.response :as response]))

(def app
  (router/router
    (router/routes
      "GET /**" [path]
      (response/file-response (str "_site/" path)))))

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
