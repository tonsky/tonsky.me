(ns site.render
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [site.core :as core])
  (:import
    [java.io File]))

(defn escape ^String [^String s]
  (when s
    (-> s
      (str/replace "&"  "&amp;")
      (str/replace "<"  "&lt;")
      (str/replace ">"  "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'"  "&#x27;"))))

(defmacro append [^StringBuilder sb & body]
  (let [sym (gensym "sb")]
    `(let [~sym ~sb]
       ~@(for [x body]
           `(.append ~sym ~x)))))

(defn render-impl [^StringBuilder sb ^String indent tree]
  (core/cond+
    (string? tree)
    (.append sb (escape tree))
     
    (nil? tree)
    :noop
     
    (and (sequential? tree) (not (vector? tree)))
    (doseq [form tree]
      (render-impl sb indent form))

    ;; assume vector now
    :let [[tag & content] tree
          [attrs content] (if (map? (first content))
                            [(first content) (next content)]
                            [{} content])]
     
    (= :raw-html tag)
    (.append sb ^String (str/join content))
     
    (and (= :script tag) (not (:processed (meta tree))))
    (let [content' (core/reindent (str/join content) (str indent "  "))]
      (append sb indent "<script>\n" content' "\n" indent "</script>\n"))
     
    :else
    (let [inline?  (#{:a :code :em :figcaption :img :span :strong} tag)
          nested?  (#{:article :blockquote :body :div :head :html :ol :script :ul} tag)
          void?    (#{:area :base :br :col :embed :hr :img :input :link :meta :param :source :track :wbr} tag)]
      ;; tag name
      (when (not inline?)
        (append sb indent))
      (append sb "<" (name tag))
      
      ;; attrs
      (doseq [[k v] attrs]
        (append sb " " (name k) "=\"")
        (append sb (escape v) "\""))
      (append sb ">")
      
      ;; insides
      (when (seq content)
        (when nested?
          (append sb "\n"))
        (render-impl sb (str indent "  ") content)
        (when nested?
          (append sb indent)))
      
      ;; close tag
      (when-not void?
        (append sb "</" (name tag) ">"))
      (when (not inline?)
        (append sb "\n")))))

(defn render [tree]
  (let [sb (StringBuilder.)]
    (render-impl sb "" tree)
    (str "<!doctype html>\n" sb)))

