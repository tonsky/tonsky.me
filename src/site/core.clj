(ns site.core
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as str])
  (:import
    [java.time LocalDate]
    [java.time.format DateTimeFormatter]))

(def server-ip
  "localhost")

(def server-port
  8080)

(def dev?
  false)

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

(defn parse-date [s format]
  (when s
    (LocalDate/parse
      s
      (DateTimeFormatter/ofPattern format))))

(defn format-date [^LocalDate date format]
  (when date
    (.format date
      (DateTimeFormatter/ofPattern format))))

(defn rsort-by [keyfn xs]
  (sort-by keyfn #(compare %2 %1) xs))
