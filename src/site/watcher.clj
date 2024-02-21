(ns site.watcher
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [site.core :as core])
  (:import
    [java.io File]
    [java.nio.file FileSystems Path StandardWatchEventKinds WatchEvent WatchEvent$Kind WatchService]))

(def *callbacks
  (atom {}))

(defn path ^Path [x & xs]
  (Path/of x (into-array String xs)))

(defn expand [dir]
  (->> (io/file dir)
    (.getAbsoluteFile)
    (file-seq)
    (filter #(.isDirectory ^File %))
    (mapv str)))

(defn watch-dirs [& dirs]
  (let [dirs'   (vec (mapcat expand dirs))
        service (-> (FileSystems/getDefault) .newWatchService)]
    (future
      (println "Started watcher on" (str/join ", " dirs))
      (try
        (let [keys (into {}
                     (for [dir dirs'
                           :let [key (.register (path dir) service (into-array WatchEvent$Kind [StandardWatchEventKinds/ENTRY_MODIFY]))]]
                       [key dir]))]
          (loop []
            (let [key (.take service)]
              (doseq [^WatchEvent event (.pollEvents key)
                      :let [arg (str (keys key) "/" (-> event .context str))
                            _   (core/debug "File changed:" arg)]
                      [_ callback] @*callbacks]
                (callback arg))
              (when (.reset key)
                (recur)))))
        ; (catch Throwable t
        ;   (println t)
        ;   (throw t))
        (finally
          (println "Stopped watcher"))))
    service))

(mount/defstate watcher
  :start
  (watch-dirs "site" "src")
  :stop
  (.close ^WatchService watcher))

(defn wrap-watcher [handler]
  (if core/dev?
    (fn [req]
      (if (= "/watcher" (:uri req))
        (http/as-channel req
          {:on-open
           (fn [ch]
             (println "Connected" ch)
             (swap! *callbacks assoc ch #(http/send! ch %)))
           :on-close
           (fn [ch status]
             (println "Disconnected" status ch)
             (swap! *callbacks dissoc ch))})
        (handler req)))
    handler))

(defn before-ns-unload []
  (mount/stop #'watcher))

(comment
  (watch-dirs ["site" "src"] prn)
  (swap! *callbacks assoc :prn prn)
  (swap! *callbacks dissoc :prn))
