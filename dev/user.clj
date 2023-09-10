(ns user
  (:require
    [clojure.core.server :as server]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :as ns]
    [mount.core :as mount]))

(ns/disable-reload!)

(ns/set-refresh-dirs "src" "dev" "/Users/tonsky/ws/clj-simple-router/src")

(def lock
  (Object.))

(defn position []
  (let [trace (->> (Thread/currentThread)
                (.getStackTrace)
                (seq))
        el    ^StackTraceElement (nth trace 4)]
    (str "[" (clojure.lang.Compiler/demunge (.getClassName el)) " " (.getFileName el) ":" (.getLineNumber el) "]")))

(defn p [form]
  `(let [t# (System/currentTimeMillis)
         res# ~form]
     (locking lock
       (println (str "#p" (position) " " '~form " => (" (- (System/currentTimeMillis) t#) " ms) " res#)))
     res#))

(alter-var-root #'*data-readers*
  assoc 'p #'p)

(def *reloaded
  (atom #{}))

(add-watch #'ns/refresh-tracker ::log
  (fn [_ _ _ new]
    (swap! *reloaded into (:clojure.tools.namespace.track/load new))))

(defn reload []
  (mount/stop)
  (set! *warn-on-reflection* true)
  (reset! *reloaded #{})
  (let [res (ns/refresh)]
    (if (= :ok res)
      (mount/start)
      (do
        (.printStackTrace ^Throwable res)
        (throw res))))
  (str "Ready – " (count @*reloaded) " ns" (when (> (count @*reloaded) 1) "es")))

(defn -main [& {:as args}]
  (reload)
  (let [port (parse-long (get args "--port" "5555"))]
    (server/start-server
      {:name          "repl"
       :port          port
       :accept        'clojure.core.server/repl
       :server-daemon false})
    (println "Started Socket REPL server on port" port)))
