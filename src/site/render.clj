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
    (append sb (escape tree))
     
    (nil? tree)
    :noop
     
    (and (sequential? tree) (not (vector? tree)))
    (doseq [form tree]
      (render-impl sb indent mode form))
    
    (not (vector? tree))
    (append sb (pr-str tree))

    ;; assume vector now
    :let [[tag attrs content] (core/normalize-tag tree)]
     
    (= :raw-html tag)
    (append sb (str/join content))
    
    (= :CDATA tag)
    (append sb "<![CDATA[\n" (str/join content) "\n]]>")

    (nil? tag)
    nil
    
    :else
    (let [inline?  (#{:a :code :em :figcaption :img :span :strong :sup :sub} tag)
          nested?  (#{:article :audio :blockquote :body :div :figure :head :html :ol :script :ul :video
                      :author :feed :entry :media:group} tag)
          void?    (#{:area :base :br :col :embed :hr :img :input :link :meta :param :source :track :wbr} tag)]
      ;; tag name
      (when (not inline?)
        (append sb indent))
      (append sb "<" (name tag))
      
      ;; attrs
      (doseq [[k v] attrs
              :when (some? v)]
        (cond
          (= true v)  (append sb " " (name k) "=\"\"")
          (= false v) :nop
          :else       (append sb " " (name k) "=\"" v "\"")))

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
