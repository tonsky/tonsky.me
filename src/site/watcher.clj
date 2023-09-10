(ns site.watcher
  (:import
    [java.nio.file FileSystems Path StandardWatchEventKinds WatchEvent WatchEvent$Kind]))

(defn path ^Path [x & xs]
  (Path/of x (into-array String xs)))

(defn watch-dirs [& dirs]
  (with-open [service (-> (FileSystems/getDefault) .newWatchService)]
    (let [keys (into {}
                 (for [dir dirs
                       :let [key (.register (path dir) service (into-array WatchEvent$Kind [StandardWatchEventKinds/ENTRY_MODIFY]))]]
                   [key dir]))]
      (loop []
        (let [key (.take service)]
            (doseq [^WatchEvent event (.pollEvents key)]
              (println (keys key) (-> event .context str)))
          (when (.reset key)
            (recur)))))))

(comment
  (watch-dirs
    "/Users/tonsky/ws/grumpy"
    "/Users/tonsky/ws/grumpy/src/grumpy"))