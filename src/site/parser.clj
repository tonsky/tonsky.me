(ns site.parser
  (:require
    [clojure.string :as str]
    [instaparse.core :as instaparse]))

(def parse
  (instaparse/parser
    "<root>     = (<#'\n+'> | block)+
     
     <block>    = h1 / h2 / h3 / h4 / ul / ol / code-block / blockquote / p
     h1         = <'#'> <#' +'> inline
     h2         = <'##'> <#' +'> inline
     h3         = <'###'> <#' +'> inline
     h4         = <'####'> <#' +'> inline
     ul         = uli (<'\n'> uli)*
     uli        = <#' *- +'> inline
     ol         = oli (<'\n'> oli)*
     oli        = <#' *\\d+\\. +'> inline
     code-block = <#'``` *'> lang? <#' *\n'> #'\n|(?!```).*'* <#'\n``` *(\n|$)'>
     lang       = #'[a-z]+'
     blockquote = qli (<'\n'> qli)*
     <qli>      = <#'> +'> p
     p          = inline
     
     <inline>   = (text / (raw-html | img | link | code | strong | em) / fallback)+
     <text>     = #'[^*_`\\[\\]<>!\n]+'
     strong     = <'**'> #'[^*]+' <'**'> | <'__'> #'[^_]+' <'__'>
     em         = <'*'> #'[^*]+' <'*'> | <'_'> #'[^_]+' <'_'>
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

; [ ] Parse post metadata

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
  [:a {:href href} alt])

(defn transform-img [[_ alt] [_ href]]
  [:img (cond-> {:src href}
          alt (assoc :alt alt)
          alt (assoc :title alt))])

(def transforms
  {:uli        #(vector :li %&)
   :oli        #(vector :li %&)
   :code-block transform-code-block
   :link       transform-link
   :img        transform-img})

(defn transform [tree]
  (instaparse/transform transforms tree))

(defn escape ^String [^String s]
  (-> s
    (str/replace "&"  "&amp;")
    (str/replace "<"  "&lt;")
    (str/replace ">"  "&gt;")
    (str/replace "\"" "&quot;")
    (str/replace "'"  "&#x27;")))

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
     
     (vector? tree)
     (let [[tag & rest] tree
           [attrs rest] (if (map? (first rest))
                          [(first rest) (next rest)]
                          [nil rest])
           newline? (not (#{:a :img :em :strong :code} tag))
           nested?  (#{:html :body :div :ul :ol :blockquote} tag)]
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
       (if (seq rest)
         (do
           (.append sb ">")
           (when nested?
             (.append sb "\n"))
           (render sb (str indent "  ") rest)
           (when nested?
             (.append sb indent))
           (.append sb "</")
           (.append sb (name tag))
           (.append sb ">")
           (when newline?
             (.append sb "\n")))
         (.append sb " />")))
     
     (sequential? tree)
     (doseq [form tree]
       (render sb indent form)))))

(defn page [tree]
  [:html {:lang "en", :prefix "og: http://ogp.me/ns#", :xmlns:og "http://opengraphprotocol.org/schema/"}
   [:head
    [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
    [:link {:href "/style.css", :rel "stylesheet", :type "text/css"}]]
   [:body
    [:div {:class "page"}
     [:article {:class "post"}
      tree]]]])
