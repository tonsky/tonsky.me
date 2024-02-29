(ns site.core
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as str]
    [mount.core :as mount])
  (:import
    [java.io File]
    [java.time Instant LocalDate ZoneId ZonedDateTime]
    [java.time.format DateTimeFormatter]
    [java.time.temporal Temporal TemporalAccessor]
    [java.util Date Timer TimerTask]))

(def server-ip
  "localhost")

(def server-port
  8080)

(def dev?
  false)

(def ^ZoneId UTC
  (ZoneId/of "UTC"))

(def atom-date-format
  "yyyy-MM-dd'T'HH:mm:ssX")

(defn apply-args [args]
  (let [args (apply array-map args)]
    (when-some [ip (args "--ip")]
      (alter-var-root #'server-ip (constantly ip)))
    (when-some [port (args "--port")]
      (alter-var-root #'server-port (constantly (parse-long port))))
    (when (= "true" (args "--dev"))
      (alter-var-root #'dev? (constantly true)))))

(defmacro cond+ [& clauses]
  (when-some [[test expr & rest] clauses]
    (case test
      :let `(let ~expr (cond+ ~@rest))
      :do  `(do ~expr (cond+ ~@rest))
      `(if ~test ~expr (cond+ ~@rest)))))

(defn concatv [& args]
  (vec (apply concat args)))

(defn consv [& args]
  (concatv
    (butlast args)
    (last args)))

(defn reindent ^String [s indent]
  (let [lines    (str/split-lines s)
        butfirst (->> lines
                   next
                   (remove str/blank?))]
    (if (seq butfirst)
      (let [prefix (->> butfirst
                     (map #(count (second (re-matches #"( *).*" %))))
                     (reduce min))]
        (str/join "\n"
          (cons
            (str indent (first lines))
            (map #(if (str/blank? %) "" (str indent (subs % prefix))) (next lines)))))
      s)))

(defn ml [s]
  (assert (string? s))
  (reindent s ""))

(defn sh [& args]
  (let [{:keys [exit] :as res} (apply shell/sh args)]
    (if (= 0 exit)
      res
      (throw
        (ex-info
          (str "External process failed: " (str/join " " args) " returned " exit)
          (assoc res :command args))))))

(defn today []
  (LocalDate/now UTC))

(defn now []
  (ZonedDateTime/now UTC))

(defn memoize-by [k f]
  (let [*cache (volatile! {})
        *date  (volatile! (today))]
    (fn [& args]
      (let [today   (today)
            _       (when (not= @*date today)
                      (vreset! *cache {})
                      (vreset! *date today))
            [kv fv] (@*cache args)
            kv'     (apply k args)]
        (if (and kv fv (= kv kv'))
          fv
          (let [fv' (apply f args)]
            (vswap! *cache assoc args [kv' fv'])
            fv'))))))

(defn update-some [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(defn some-map [& args]
  (reduce
    (fn [m [k v]]
      (if (some? v)
        (assoc m k v)
        m))
    {} (partition 2 args)))

(defn map-by [f xs]
  (into {} (map #(vector (f %) %)) xs))

(defn max-by [cmp]
  (fn [x y]
    (if (>= (cmp x y) 0) x y)))

(defn reverse-compare [x y]
  (compare y x))

(defn pluralize [x singular plural]
  (if (= x 1)
    (str/replace singular "{1}" (str x))
    (str/replace plural "{1}" (str x))))

(defn median [xs]
  (if (odd? (count xs))
    (nth xs (-> xs count (quot 2)))
    (/ (+
         (nth xs (-> xs count (quot 2) dec))
         (nth xs (-> xs count (quot 2))))
      2)))

(defn replace+
  "Replace parts of string with arbitrary data structures (e.g. Hiccup)"
  [s re f]
  (let [m (re-matcher re s)]
    (loop [res  []
           last 0]
      (if (.find m)
        (recur
          (-> res
            (conj (subs s last (.start m)))
            (conj (f (subs s (.start m) (.end m)))))
          (.end m))
        (-> res
          (conj (subs s last))
          (->> (remove #(= "" %)))
          (seq))))))

(defn normalize-tag [form]
  (when (vector? form)
    (let [[tag & content] form
          [attrs content] (if (map? (first content))
                            [(first content) (next content)]
                            [{} content])
          [tag attrs] (reduce
                        (fn [[tag attrs] s]
                          (cond
                            (str/starts-with? s ".")
                            [tag (update attrs :class #(if % (str % " " (subs s 1)) (subs s 1)))]
                          
                            (str/starts-with? s "#")
                            [tag (assoc attrs :id (subs s 1))]
                          
                            :else
                            [(keyword s) attrs]))
                        [:div attrs] (re-seq #"[.#]?[a-zA-Z0-9\-_:]+" (name tag)))]
      [tag attrs content])))

(defmacro transform-tag [page [tag attr-sym content-sym] & body]
  `(let [page# ~page
         tag#  ~tag]
     (assoc page# :content
       (walk/postwalk 
         (fn [form#]
           (or
             (when-some [[tag2# ~attr-sym ~content-sym] (normalize-tag form#)]
               (when (= tag2# tag#)
                 ~@body))
             form#))
         (:content page#)))))

(defn parse-date ^LocalDate [s format]
  (when s
    (LocalDate/parse
      s
      (DateTimeFormatter/ofPattern format))))

(defn format-temporal [^TemporalAccessor ta format]
  (when ta
    (let [^DateTimeFormatter format (cond-> format
                                      (string? format) DateTimeFormatter/ofPattern)
          format (.withZone format UTC)]
      (.format format ta))))

(defn rsort-by [keyfn xs]
  (sort-by keyfn #(compare %2 %1) xs))

(defn file [& args]
  (let [^File file (apply io/file args)]
    (when (.exists file)
      file)))

(def list-files
  (memoize-by
    #(.lastModified (io/file %))
    #(.listFiles (io/file %))))

(defn resize [^File file w h]
  (let [[w h] (cond (str/index-of (.getName file) "@2x") [(quot w 2) (quot h 2)]
                (>= w 1088)                           [(quot w 2) (quot h 2)]
                (> h 700)                             [(quot w 2) (quot h 2)]
                :else                                 [w h])]
    (if (str/index-of (.getName file) "@hover")
      [w (quot h 2)]
      [w h])))

(defn svg-dimensions [file]
  (let [body       (slurp file)
        head       (re-find #"<svg[^>]+>" body)
        [_ width]  (re-find #"width=['\"]([0-9.\-]+)['\"]" head)
        [_ height] (re-find #"height=['\"]([0-9.\-]+)['\"]" head)]
    (when (and width height)
      [(edn/read-string width) (edn/read-string height)])))

(def image-dimensions
  (memoize-by
    #(.lastModified (io/file %))
    (fn [path]
      (try
        (let [file (io/file path)]
          (when (.exists file)
            (or
              (when (str/ends-with? (.getName file) ".svg")
                (svg-dimensions file))
              (let [out   (:out (sh "convert" (.getPath file) "-ping" "-format" "[%w,%h]" "info:"))
                    [w h] (edn/read-string out)]
                (resize file w h)))))
        (catch Exception e
          (.printStackTrace e)
          nil)))))

(def video-dimensions
  (memoize-by
    #(.lastModified (io/file %))
    (fn [path]
      (let [file (io/file path)]
        (when (.exists file)
          (let [out  (:out (sh "ffprobe" "-v" "error" "-select_streams" "v" "-show_entries" "stream=width,height" "-of" "csv=p=0:s=x" (.getPath file)))
                parse-long #(if (= "N/A" %)
                              nil
                              (Long/parseLong %))]
            (when-some [[_ w h] (re-find #"^(\d+|N/A)x(\d+|N/A)" out)]
              (let [w (parse-long w)
                    h (parse-long h)]
                (when (and w h)
                  (resize file w h))))))))))

(defn youtube-id [url]
  (->> url
    (re-matches #"https?://(?:www\.)?(?:youtube\.com|youtu\.be)/.*[?&]v=([a-zA-Z0-9_\-]{11}).*")
    second))

(defn vimeo-id [url]
  (->> url
    (re-matches #"https?://vimeo.com/([0-9]+)")
    second))

(defn timestamp-url [url file]
  (let [file (io/file file)]
    (if (.exists file)
      (let [modified (quot (.lastModified file) 1000)]
        (str url (if (str/index-of url "?") "&" "?") "t=" modified))
      url)))

(mount/defstate ^Timer timer
  :start (Timer. true)
  :stop  (.cancel ^Timer timer))

(defn- timer-task ^TimerTask [f]
  (proxy [TimerTask] []
    (run []
      (try
        (f)
        (catch Throwable t
          (.printStackTrace t))))))

(defn schedule
  ([f ^long delay]
   (let [t (timer-task f)]
     (.schedule timer t delay)
     t))
  ([f ^long delay ^long period]
   (let [t (timer-task f)]
     (.schedule timer t delay period)
     t)))

(defn schedule-once [f ^Temporal temporal]
  (let [date (Date. (* 1000 (.getLong temporal java.time.temporal.ChronoField/INSTANT_SECONDS)))
        t    (timer-task f)]
    (.schedule timer t date)
    t))

(defn cancel-task [^TimerTask task]
  (.cancel task))

(defmacro debug [& args]
  `(when dev?
     (println ~@args)))

(defmacro measure [name & body]
  `(if dev?
     (let [t#   (System/currentTimeMillis)
           res# (do ~@body)]
       (println (format "[ %4d ms ] %s" (- (System/currentTimeMillis) t#) ~name))
       res#)
     (do ~@body)))

(defn before-ns-unload []
  (mount/stop #'timer))
