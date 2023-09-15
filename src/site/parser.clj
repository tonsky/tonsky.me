(ns site.parser
  (:require
    [clojure.string :as str]
    [instaparse.core :as instaparse]))

(def parse
  (instaparse/parser
    "<root>       = meta? (<'\n'> | block <#' *(\n|$)'>)+
     
     meta         = <#'--- *\n'> meta-item* <#'--- *\n'>
     meta-item    = meta-key <#' *: *'> (<'\\''> meta-value <'\\''> | <'\"'> meta-value <'\"'> | meta-value) <'\n'>
     <meta-key>   = #'[a-zA-Z0-9_\\-\n]+'
     <meta-value> = #'[^\"\n]*'
     
     <block>    = h1 / h2 / h3 / h4 / ul / ol / code-block / blockquote / figure / p
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
     figure     = <#' *'> #'(?i)[^\n]+\\.(png|jpg|jpeg|gif|webp)' <#' *'> caption?
     <caption>  = <#'\n *'> #'[^ \n][^\n]*'
     p          = inline
     
     <inline>   = (text / raw-html / img / link / code / strong / em / fallback)+
     <text>     = #'[^*_`\\[\\]<>!\n]+'
     strong     = <'**'> #'[^*\n]+' <'**'> | <'__'> #'[^_\n]+' <'__'>
     em         = <'*'> #'[^*\n]+' <'*'> | <'_'> #'[^_\n]+' <'_'>
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
  [:a {:href href} alt])

(defn transform-img [[_ alt] [_ href]]
  [:img (cond-> {:src href}
          alt (assoc :alt alt)
          alt (assoc :title alt))])

(def transforms
  {:uli        #(vector :li %&)
   :oli        #(vector :li %&)
   :code-block transform-code-block
   :figure     (fn
                 ([url]
                  [:figure
                   [:img {:src url}]])
                 ([url caption]
                  [:figure
                   [:img {:src url}]
                   [:figcaption caption]]))
   :link       transform-link
   :img        transform-img
   :meta-item  #(vector (keyword %1) %2)
   :meta       #(into {} %&)})

(defn transform [tree]
  (let [content (instaparse/transform transforms tree)
        [meta content] (if (map? (first content))
                         [(first content) (next content)]
                         [nil content])]
    (assoc meta :content content)))
