(ns site.parser
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [instaparse.core :as instaparse]
    [site.core :as core])
  (:import
    [java.io File]))

(def ^:dynamic *dir*
  nil)

(defmethod clojure.core/print-method instaparse.core.Parser [x writer]
  (if *print-readably*
    (print-method (instaparse.print/Parser->str x) writer)
    (binding [*out* writer]
      (println (instaparse.print/Parser->str x)))))

(defmethod clojure.core/print-method instaparse.gll.Failure [x ^java.io.Writer w]
  (if *print-readably*
    (print-method (with-out-str (instaparse.failure/pprint-failure x)) w)
    (instaparse.failure/pprint-failure x)))

(def parse
  (instaparse/parser
    "<root>       = meta? (block? <#' *(\n|$)'>)+
     
     meta         = <#'--- *\n'> meta-item* <#'--- *\n'>
     meta-item    = meta-key <#' *: *'> (<'\\''> meta-value <'\\''> | <'\"'> meta-value <'\"'> | meta-value) <'\n'>
     <meta-key>   = #'[a-zA-Z0-9_\\-\n]+'
     <meta-value> = #'[^\"\n]*'
     
     <block>    = h1 / h2 / h3 / h4 / ul / ol / code-block / blockquote / figure / video / p
     h1         = <'#'> <#' +'> inline
     h2         = <'##'> <#' +'> inline
     h3         = <'###'> <#' +'> inline
     h4         = <'####'> <#' +'> inline
     ul         = uli (<#'\n\n?'> uli)*
     uli        = <#' *- +'> inline
     ol         = oli (<#'\n\n?'> oli)*
     oli        = <#' *\\d+\\. +'> inline
     code-block = <#'``` *'> lang? <#' *\n'> #'\n|(?!```).*'* <#'\n``` *(\n|$)'>
     lang       = #'[a-z]+'
     blockquote = qli (<'\n'> qli)*
     <qli>      = <#'> +'> p
     figure     = <#' *'> #'(?i)[^ \n]+\\.(png|jpg|jpeg|gif|webp)' figlink? figalt? figcaption?
     video      = <#' *'> #'(?i)[^ \n]+\\.(mp4|webm)' figlink? figalt? figcaption?
     figlink    = <#' +'> #'https?://[^ \n]+'
     figalt     = <#' +'> !'https://' #'[^ \n][^\n]*[^ \n]'
     figcaption = <#' *\n *'> #'[^ \n][^\n]*'
     p          = class+ <#' *'> / class+ <#' +'> pbody / class+ <#' +'> inline / <#' *'> inline
     class      = <'.'> #'[a-zA-Z0-9_\\-]+'
     <pbody>    = !'.' inline
     
     <inline>   = (text / raw-html / img / link / code / strong / em / fallback)+
     <text>     = #'[^*_`\\[\\]<>!\n]+'
     strong     = <#'(?<!\\w)\\*\\*'> #'([^*\n]|\\*\\w)+' <#'\\*\\*(?!\\w)'> | <'__'> #'[^_\n]+' <'__'>
     em         = <#'(?<!\\w)\\*'> #'([^*\n]|\\*\\w)+' <#'\\*(?!\\w)'> | <#'(?<!\\w)_'> #'([^_\n]|_\\w)+' <#'_(?!\\w)'>
     code       = <'`'> #'(\\\\`|[^`])*' <'`'>
     alt        = (#'[^*`\\]]+' / (strong | em | code) / #'[^\\]]+')*
     href       = #'[^\\)]*'
     img        = <'!['> alt <']('> href <')'>
     link       = <'['> alt <']('> href <')'>
     <fallback> = #'[*_`\\[\\]<>!]'
     
     raw-html           = void-tag | self-closing-tag | tag
     <inner-html>       = (html-text | void-tag | self-closing-tag | tag)*
     <html-text>        = #'[^<]+'
     <tag>              = open-tag inner-html close-tag
     <open-tag>         = #'< *' tag-name #' [^>]+'? '>'
     <close-tag>        = #'</ *' tag-name #' *>'
     <tag-name>         = #'[a-zA-Z0-9_\\-]+'
     <self-closing-tag> = #'< *' tag-name #'( [^/>]+)?' '/>'
     <void-tag>         = #'< *' void-tag-name #' [^>]+'? '>'
     <void-tag-name>    = 'area' | 'base' | 'br' | 'col' | 'embed' | 'hr' | 'img' | 'input' | 'link' | 'meta' | 'param' | 'source' | 'track' | 'wbr'"))

(defn transform-code-block [& args]
  (let [[lang content]
        (if (vector? (first args))
          [(second (first args)) (next args)]
          [nil args])]
    [:pre
     (vec
       (concat
         [:code]
         (when lang
           [{:data-lang lang}])
         content))]))

(defn transform-link [[_ alt] [_ href]]
  [:a {:href   href
       :target (when (and 
                       (str/starts-with? href "http")
                       (not (str/starts-with? href "https://tonsky.me")))
                 "_blank")}
   alt])

(defn transform-img [[_ alt] [_ href]]
  (let [file  (io/file *dir* href)
        [w h] (core/image-dimensions file)]
    [:img
     {:class "inline"
      :src   (core/timestamp-url href file)
      :width  w
      :height h
      :alt    alt
      :title  alt}]))

(defn normalize-figure [args]
  (reduce
    (fn [m [tag value]]
      (assoc m tag value))
    {}
    args))

(defn transform-figure [url & args]
  (let [{:keys [figlink figalt figcaption]} (normalize-figure args)
        file  (io/file *dir* url)
        [w h] (core/image-dimensions file)
        img   [:img {:src    (core/timestamp-url url file)
                     :width  w
                     :height h
                     :alt    figalt
                     :title  figalt}]]
    [:figure
     (if figlink
       [:a {:href figlink :target "_blank"} img]
       img)
     (when figcaption
       [:figcaption figcaption])]))

(defn transform-video [url & args]
  (let [{:keys [figlink figalt figcaption]} (normalize-figure args)
        [_ ext] (re-matches #".*\.([a-z0-9]+)" url)
        file    (io/file *dir* url)
        [w h]   (core/video-dimensions file)
        source  [:source 
                 {:src  (core/timestamp-url url file)
                  :type (str "video/" ext)}]
        video   [:video
                 {:autoplay    true
                  :muted       true
                  :loop        true
                  :preload     "auto"
                  :playsinline true
                  :controls    true
                  :width       w
                  :height      h}
                 source]]
    [:figure
     (if figlink
       [:a {:href figlink :target "_blank"} video]
       video)
     (when figcaption
       [:figcaption figcaption])]))

(defn replace+ [s re f]
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

(defn detect-links [form]
  (core/cond+
    (string? form)
    (let [form' (replace+ form #"https?://[^ ]+[^!()-,.<>:;'\" ?&]"
                  (fn [s] [:a {:href s
                               :target "_blank"}
                           (str/replace s #"^https?://" "")]))]
      (if (= (first form') form)
        form
        form'))
    
    (and (sequential? form) (not (vector? form)))
    (apply list (map detect-links form))
    
    (not (vector? form))
    form
    
    :let [[tag attrs content] (core/normalize-tag form)]
    
    (#{:a :img :video :code :code-block} tag)
    form
    
    :else
    (vec
      (concat
        [tag attrs]
        (map detect-links content)))))

(defn transform-paragraph [& body]
  (let [[classes body] (split-with #(and (vector? %) (= :class (first %))) body)
        classes (map second classes)
        body  (if (seq classes)
                (cons {:class (str/join " " classes)} body)
                body)]
    (detect-links (vec (cons :p body)))))

(def transforms
  {:uli        #(vector :li %&)
   :oli        #(vector :li %&)
   :code-block transform-code-block
   :figure     transform-figure
   :video      transform-video
   :link       transform-link
   :img        transform-img
   :p          transform-paragraph
   :meta-item  #(vector (keyword %1) %2)
   :meta       #(into {} %&)})

(defn transform [tree]
  (let [content (instaparse/transform transforms tree)
        [meta content] (if (map? (first content))
                         [(first content) (next content)]
                         [nil content])]
    (assoc meta :content (doall content))))

(def just-parse
  (core/memoize-by
    #(.lastModified ^File %)
    (fn [path]
      (parse (slurp (io/file path))))))

(def parse-md
  (core/memoize-by
    (fn [path]
      (transduce (map #(.lastModified ^File %)) max 0 (-> path io/file .getParentFile file-seq)))
    (fn [path]
      (let [file (io/file path)
            name (.getName file)
            id   (if (= name "index.md")
                   (.getName (.getParentFile file))
                   (second (re-matches #"\d+-\d+-\d+-(.*)\.md" name)))]
        (core/measure
          (str "Parsing " (.getName (.getParentFile file)) "/" (.getName file))
          (binding [*dir* (.getParentFile file)]
            (-> file
              just-parse
              transform
              (assoc 
                :url (str "/blog/" id "/")
                :categories #{:blog}))))))))
