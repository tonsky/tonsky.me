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

(defn render-impl [^StringBuilder sb ^String indent mode tree]
  (core/cond+
    (string? tree)
    (.append sb (escape tree))
     
    (nil? tree)
    :noop
     
    (and (sequential? tree) (not (vector? tree)))
    (doseq [form tree]
      (render-impl sb indent mode form))

    ;; assume vector now
    :let [[tag attrs content] (core/normalize-tag tree)]
     
    (= :raw-html tag)
    (append sb (str/join content))
    
    (= :CDATA tag)
    (append sb "<![CDATA[\n" (str/join content) "\n]]>")
     
    (and (= :script tag) (not (:processed (meta tree))))
    (let [content' (core/reindent (str/join content) (str indent "  "))]
      (append sb indent "<script>\n" content' "\n" indent "</script>\n"))
     
    :else
    (let [inline?  (#{:a :code :em :figcaption :img :span :strong} tag)
          nested?  (#{:article :blockquote :body :div :figure :head :html :ol :script :ul :video
                      :author :feed :entry} tag)
          void?    (#{:area :base :br :col :embed :hr :img :input :link :meta :param :source :track :wbr} tag)]
      ;; tag name
      (when (not inline?)
        (append sb indent))
      (append sb "<" (name tag))
      
      ;; attrs
      (doseq [[k v] attrs
              :when (some? v)]
        (append sb " " (name k) "=\"")
        (append sb (if (= v true) "" (escape v)) "\""))

      ;; insides
      (when (seq content)
        (append sb ">")
        (when nested?
          (append sb "\n"))
        (render-impl sb (str indent "  ") mode content)
        (when nested?
          (append sb indent)))
      
      ;; close tag
      (cond
        (seq content) (append sb "</" (name tag) ">")
        (= :xml mode) (append sb " />")
        void?         (append sb ">")
        (not void?)   (append sb "></" (name tag) ">"))
      
      (when (not inline?)
        (append sb "\n")))))

(defn render-inner-html [tree]
  (let [sb (StringBuilder.)]
    (render-impl sb "" :html tree)
    (str sb)))

(defn render-html [tree]
  (let [sb (StringBuilder.)]
    (append sb "<!doctype html>\n")
    (render-impl sb "" :html tree)
    (str sb)))

(defn render-xml [tree]
  (let [sb (StringBuilder.)]
    (append sb "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    (render-impl sb "" :xml tree)
    (str sb)))
