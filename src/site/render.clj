(ns site.render
  (:require
    [clojure.string :as str]))

(defn escape ^String [^String s]
  (when s
    (-> s
      (str/replace "&"  "&amp;")
      (str/replace "<"  "&lt;")
      (str/replace ">"  "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'"  "&#x27;"))))

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

(defn render
  ([tree]
   (let [sb (StringBuilder.)]
     (render sb "" tree)
     (str "<!doctype html>\n" sb)))
  ([^StringBuilder sb ^String indent tree]
   (cond
     (string? tree)
     (.append sb (escape tree))
     
     (and (vector? tree) (= :raw-html (first tree)))
     (.append sb ^String (str/join (next tree)))
     
     (and (vector? tree) (= :script (first tree)))
     (let [script (str/join (next tree))]
       (.append sb indent)
       (.append sb "<script>\n")
       (.append sb (reindent script (str indent "  ")))
       (.append sb "\n")
       (.append sb indent)
       (.append sb "</script>\n"))
     
     (vector? tree)
     (let [[tag & rest] tree
           [attrs rest] (if (map? (first rest))
                          [(first rest) (next rest)]
                          [nil rest])
           newline? (not (#{:a :img :em :strong :code} tag))
           nested?  (#{:article :blockquote :body :div :head :html :ol :script :ul} tag)
           void?    (#{:area :base :br :col :embed :hr :img :input :link :meta :param :source :track :wbr} tag)]
       (when newline?
         (.append sb indent))
       (.append sb "<")
       (.append sb (name tag))
       (doseq [[k v] attrs]
         (.append sb " ")
         (.append sb (name k))
         (.append sb "=\"")
         (.append sb (escape v))
         (.append sb "\""))

       (.append sb ">")
       (when (seq rest)
         (when nested?
           (.append sb "\n"))
         (render sb (str indent "  ") rest)
         (when nested?
           (.append sb indent)))
       (when-not void?
         (.append sb "</")
         (.append sb (name tag))
         (.append sb ">"))
       (when newline?
         (.append sb "\n")))
     
     (sequential? tree)
     (doseq [form tree]
       (render sb indent form)))))
