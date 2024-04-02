(ns site.parser
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [instaparse.core :as instaparse]
    [site.cache :as cache]
    [site.core :as core])
  (:import
    [java.io File]))

(def ^:dynamic *path*
  nil)

(def ^:dynamic *meta*)

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
    "<root>       = meta? !'---' (block? <#' *(\n|$)'>)*
     
     meta         = <#'--- *\n'> meta-item* <#'--- *(\n|$)'>
     meta-item    = meta-key <#' *: *'> (<'\\''> meta-value <'\\''> | <'\"'> meta-value <'\"'> | meta-value) <'\n'>
     <meta-key>   = #'[a-zA-Z0-9_\\-\n]+'
     <meta-value> = #'[^\"\n]*'
     
     <block>    = h1 / h2 / h3 / h4 / ul / ol / fl / code-block / blockquote / figure / video / youtube / raw-html / p
     h1         = <'#'> <#' +'> inline
     h2         = <'##'> <#' +'> inline
     h3         = <'###'> <#' +'> inline
     h4         = <'####'> <#' +'> inline
     ul         = uli (<#'\n\n?'> uli)*
     uli        = <#' *[-\\*] +'> inline
     ol         = oli (<#'\n\n?'> oli)*
     oli        = <#' *'> #'\\d+' <#'\\. +'> inline
     fl         = fli (<#'\n\n?'> fli)*
     fli        = <'[^'> #'\\d+' <']: '> inline
     
     code-block = <#'``` *'> lang? <#' *\n'> #'\n|(?!```).*'* <#'\n``` *(\n|$)'>
     lang       = #'[a-z]+'
     blockquote = qli (<'\n'> qli)*
     <qli>      = <#'> +'> p
     figure     = <#' *'> #'(?i)[^ \n]+\\.(png|jpg|jpeg|gif|webp)' figlink? figalt? figcaption?
     video      = <#' *'> #'(?i)[^ \n]+\\.(mp4|webm)' figlink? figalt? figcaption?
     youtube    = <#'(?i) *https?://(www\\.)?(youtube\\.com|youtu\\.be)/[^ \n]+[?&]v='> #'[a-zA-Z0-9_\\-]{11}' <#'[^ \n]*'> figcaption? figattrs*
     figlink    = <#' +'> #'https?://[^ \n]+'
     figalt     = <#' +'> !'https://' #'[^ \n][^\n]*[^ \n]'
     figcaption = <#' *\n *'> inline
     figattrs   = <#' +'> figattr <'='> figattr
     <figattr>  = #'[a-zA-Z0-9_\\-\\.]+'
     p          = class+ <#' *'> / class+ <#' +'> pbody / class+ <#' +'> inline / <#' *'> inline
     class      = <'.'> #'[a-zA-Z0-9_\\-]+'
     <pbody>    = !'.' inline
     
     <inline>   = (text / raw-html / img / link / code / s / strong / em / footnote/ fallback)+
     <text>     = <'\\\\'> #'.' / #'[^*_~`\\[\\]<>!\\\\\n]+'
     s          = <'~~'> inline <#'\\~~(?!\\w)'>
     strong     = <'**'> inline <#'\\*\\*(?!\\w)'> | <'__'> inline <#'__(?!\\w)'>
     em         = <'*'> inline <#'\\*(?!\\w)'> | <'_'> inline <#'_(?!\\w)'>
     code       = <'`'> #'(\\\\`|[^`])*' <'`'>
     alt        = (#'[^*`\\]]+' / (strong | em | code) / #'[^\\]]+')*
     href       = href_inn
     <href_inn> = ('(' href_inn ')' | #'[^\\(\\)]*')*
     img        = <'!['> alt <']('> href <')'>
     link       = <'['> alt <']('> href <')'>
     footnote   = <'[^'> #'\\d+' <']'>
     <fallback> = #'[*_~`\\[\\]<>!\\\\]'
     
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

(defn inner-text [form]
  (cond
    (string? form) form
    (vector? form) (str/join "" (keep inner-text (next form)))
    (sequential? form) (str/join "" (keep inner-text form))
    :else nil))

(defn transform-header [tag & body]
  (let [id (-> (inner-text body)
             (str/lower-case)
             (str/replace #" " "-")
             (str/replace #"[^a-z0-9\-]" ""))]
    (core/consv tag {:id id} body)))

(defn transform-code-block [& args]
  (let [[lang content]
        (if (vector? (first args))
          [(second (first args)) (next args)]
          [nil args])]
    [:pre
     (core/concatv
       [:code]
       (when lang
         [{:data-lang lang}])
       content)]))

(defn transform-link [[_ alt] [_ href]]
  [:a {:href href} alt])

(defn transform-img [[_ alt] [_ href]]
  [:img
   (core/some-map
     :class  "inline"
     :src    href
     :alt    alt
     :title  alt)])

(defn normalize-figure [args]
  (let [m (reduce
            (fn [m [tag & values]]
              (assoc m tag values))
            {}
            args)]
    (reduce
      (fn [m [k v]]
        (assoc m (keyword k) v))
      (dissoc m :figattrs) (partition 2 (:figattrs m)))))

(defn transform-figure [url & args]
  (let [{[figlink]  :figlink
         [figalt]   :figalt
         figcaption :figcaption} (normalize-figure args)
        img [:img (core/some-map
                    :src    url
                    :class  (when (re-find #"@hover" url) "hoverable")
                    :alt    figalt
                    :title  figalt)]]
    [:figure
     (if figlink
       [:a {:href figlink} img]
       img)
     (when figcaption
       (core/consv :figcaption figcaption))]))

(defn transform-video [url & args]
  (let [{[figlink]  :figlink
         [figalt]   :figalt
         figcaption :figcaption} (normalize-figure args)
        [_ ext] (re-matches #".*\.([a-z0-9]+)" url)
        source  [:source 
                 {:src  url
                  :type (str "video/" ext)}]
        opts    (:video @*meta* "")
        video   [:video
                 {:autoplay    (not (str/index-of opts "-autoplay"))
                  :muted       (not (str/index-of opts "-muted"))
                  :loop        (not (str/index-of opts "-loop"))
                  :preload     "auto"
                  :playsinline (not (str/index-of opts "-playsinline"))
                  :controls    (not (str/index-of opts "-controls"))}
                 source]]
    [:figure
     (if figlink
       [:a {:href figlink} video]
       video)
     (when figcaption
       [:figcaption figcaption])]))

(defn transform-youtube [id & args]
  (let [{:keys [figcaption aspect]} (normalize-figure args)
        aspect (or (some-> aspect parse-double) 16/9)
        width  635]
    [:figure
     ;; TODO fetch width/height from YouTube
     [:iframe {:width           (str width)
               :height          (-> width (/ aspect) math/ceil int str)
               :src             (str "https://www.youtube-nocookie.com/embed/" id)
               :frameborder     "0"
               :allow           "autoplay; encrypted-media; picture-in-picture"
               :allowfullscreen true}]
     (when figcaption
       [:figcaption figcaption])]))

(defn detect-links [form]
  (core/cond+
    (string? form)
    (let [form' (core/replace+ form #"https?://[^ ]+[^!()-,.<>:;'\" ?&]"
                  (fn [s] [:a {:href s}
                           (str/replace s #"^https?://" "")]))]
      (if (= (first form') form)
        form
        form'))
    
    (and (sequential? form) (not (vector? form)))
    (apply list (map detect-links form))
    
    (not (vector? form))
    form
    
    :let [[tag attrs content] (core/normalize-tag form)]
    
    (#{:a :img :code :code-block :raw-html :video} tag)
    form
    
    (empty? attrs)
    (core/consv tag (map detect-links content))
    
    :else
    (core/consv tag attrs (map detect-links content))))

(defn transform-href [& body]
  [:href (str/join body)])

(defn transform-paragraph [& body]
  (let [[classes body] (split-with #(and (vector? %) (= :class (first %))) body)
        classes (map second classes)
        body  (if (seq classes)
                (cons {:class (str/join " " classes)} body)
                body)]
    (detect-links (core/consv :p body))))

(defn transform-footnote [id]
  [:sup {:id (str "fnref:" id) :role "doc-noteref"}
   [:a {:href (str "#fn:" id) :class "footnote" :rel "footnote"}
    id]])

(defn transform-ol [& body]
  (let [start (-> body first second)]
    (core/consv
      :ol {:start start}
      (map (fn [[_ _ & rest]] (core/consv :li rest)) body))))

(defn transform-fl [& body]
  (list
    [:div {:class "footnotes-br"}
     [:div {:class "footnotes-br_inner"}]]
    (core/consv :ol {:class "footnotes" :role "doc-endnotes"} body)))

(defn transform-fli [id & body]
  [:li {:id (str "fn:" id) :role "doc-endnote"}
   body
   [:a {:class "reversefootnote" :href (str "#fnref:" id) :role "doc-backlink"}
    "↩"]])

(defn transform-meta [& items]
  (let [meta (into {} items)]
    (reset! *meta* meta)
    meta))

(def transforms
  {:h1         #(transform-header :h1 %&)
   :h2         #(transform-header :h2 %&)
   :h3         #(transform-header :h3 %&)
   :uli        #(core/consv :li %&)
   :ol         transform-ol
   :fl         transform-fl
   :fli        transform-fli
   :code-block transform-code-block
   :figure     transform-figure
   :video      transform-video
   :youtube    transform-youtube
   :link       transform-link
   :img        transform-img
   :href       transform-href
   :p          transform-paragraph
   :footnote   transform-footnote
   :meta-item  #(vector (keyword %1) %2)
   :meta       transform-meta})

(defn transform [tree]
  (binding [*meta* (atom nil)]
    (let [content (instaparse/transform transforms tree)
          [meta content] (if (map? (first content))
                           [(first content) (next content)]
                           [nil content])]
      (assoc meta :content (doall content)))))

(def parse-md
  (core/memoize-by
    #(.lastModified (cache/touched (io/file (str "site/" % "/index.md"))))
    (fn [path]
      (let [file (io/file (str "site/" path "/index.md"))]
        (core/measure
          (str "Parsing " (str file))
          (binding [*path* path]
            (-> file
              slurp
              parse
              transform
              (assoc 
                :uri path
                :categories #{:blog}))))))))
