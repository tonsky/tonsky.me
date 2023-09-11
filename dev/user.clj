(ns user
  (:require
    [clojure.core.server :as server]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :as ns]
    [mount.core :as mount]))

(ns/disable-reload!)

(ns/set-refresh-dirs "src" "dev" "test" "/Users/tonsky/ws/clj-simple-router/src")

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

(defn ml [s]
  (assert (string? s))
  (let [lines  (str/split-lines s)
        prefix (->> lines
                 next
                 (remove str/blank?)
                 (map #(count (second (re-matches #"( *).*" %))))
                 (reduce min))]
    (str/join "\n"
      (cons
        (first lines)
        (map #(if (str/blank? %) "" (subs % prefix)) (next lines))))))

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

(defn test-all []
  (reload)
  (let [{:keys [fail error] :as res} (test/run-all-tests #"site\..*")
        res (dissoc res :type)]
    (if (pos? (+ fail error))
      (throw (ex-info "Tests failed" res))
      res)))

(defn -test [_]
  (reload)
  (let [{:keys [fail error]} (test/run-all-tests #"site\..*")]
    (System/exit (+ fail error))))
