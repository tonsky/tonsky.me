(ns site.core
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as str])
  (:import
    [java.time LocalDate ZoneId]
    [java.time.format DateTimeFormatter]
    [java.time.temporal TemporalAccessor]))

(def server-ip
  "localhost")

(def server-port
  8080)

(def dev?
  false)

(def ^ZoneId UTC
  (ZoneId/of "UTC"))

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

(defn reindent ^String [s indent]
  (let [lines  (str/split-lines s)
        prefix (->> lines
                 next
                 (remove str/blank?)
                 (map #(count (second (re-matches #"( *).*" %))))
                 (reduce min))]
    (str/join "\n"
      (cons
        (str indent (first lines))
        (map #(if (str/blank? %) "" (str indent (subs % prefix))) (next lines))))))

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

(defn memoize-by [k f]
  (let [*cache (volatile! {})]
    (fn [& args]
      (let [[kv fv] (@*cache args)
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

(defn max-by [cmp]
  (fn [x y]
    (if (>= (cmp x y) 0) x y)))

(defn normalize-tag [form]
  (when (vector? form)
    (let [[tag & content] form
          [attrs content] (if (map? (first content))
                            [(first content) (next content)]
                            [{} content])]
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

(def image-dimensions
  (memoize-by
    #(.lastModified (io/file %))
    (fn [path]
      (let [file (io/file path)]
        (when (.exists file)
          (let [out   (:out (sh "convert" (.getPath file) "-ping" "-format" "[%w,%h]" "info:"))
                [w h] (edn/read-string out)]
            (if (str/index-of (.getName file) "@2x.")
              [(quot w 2) (quot h 2)]
              [w h])))))))

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
                  (if (str/index-of (.getName file) "@2x.")
                    [(quot w 2) (quot h 2)]
                    [w h]))))))))))

(defn timestamp-url [url file]
  (let [file (io/file file)]
    (if (.exists file)
      (let [modified (quot (.lastModified file) 1000)]
        (str url (if (str/index-of url "?") "&" "?") "t=" modified))
      url)))
