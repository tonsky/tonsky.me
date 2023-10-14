(ns site.watcher
  (:require
    [clojure.java.io :as io]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [site.core :as core])
  (:import
    [java.io File]
    [java.nio.file FileSystems Path StandardWatchEventKinds WatchEvent WatchEvent$Kind WatchService]))

(def *callbacks
  (atom {}))

(def *watcher
  (atom nil))

(defn path ^Path [x & xs]
  (Path/of x (into-array String xs)))

(defn expand [dir]
  (->> (io/file dir)
    (.getAbsoluteFile)
    (file-seq)
    (filter #(.isDirectory ^File %))
    (mapv str)))

(def dirs
  (vec (mapcat expand ["site" "src"])))

(defn watch-dirs [dirs]
  (let [service (-> (FileSystems/getDefault) .newWatchService)]
    (future
      (println "Watcher started")
      (try
        (let [keys (into {}
                     (for [dir dirs
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
        (finally
          (println "Watcher stopped"))))
    service))

(add-watch *callbacks :watcher
  (fn [_ _ old new]
    ; (prn old "->" new)
    (cond
      (and (empty? old) (not (empty? new)))
      (reset! *watcher (watch-dirs dirs))
      
      (and (not (empty? old)) (empty? new))
      (swap! *watcher #(do (.close ^WatchService %) nil)))))

(def routes
  {"GET /watcher"
   (fn [req]
     (if core/dev?
       (http/as-channel req
         {:on-open
          (fn [ch]
            (swap! *callbacks assoc ch #(http/send! ch %)))
          :on-close
          (fn [ch status]
            (swap! *callbacks dissoc ch))})
       {:status 404
        :body   "Not found"}))})
   
(comment
  (watch-dirs ["site" "src"] prn)
  (swap! *callbacks assoc :prn prn)
  (swap! *callbacks dissoc :prn))
