(ns site.pages.atom
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [site.core :as core]
    [site.pages.index :as index]
    [site.parser :as parser]
    [site.render :as render])
  (:import
    [java.io File]
    [java.time LocalDate]))

(def date-format
  "yyyy-MM-dd'T'HH:mm:ssX")

(defn absolutize [url page-uri]
  (when url
    (cond
      (str/index-of url "://")
      url
      
      (str/starts-with? "./" url)
      (str "https://tonsky.me" page-uri (subs url 2))
      
      (str/starts-with? "/" url)
      (str "https://tonsky.me" url)
      
      :else
      (str "https://tonsky.me" page-uri url))))

(defn absolutize-srcset [srcset page-uri]
  (str/replace srcset #"([^ ]\.[a-z0-9]+)" #(absolutize % page-uri)))

(defn absolutize-urls [page]
  (assoc page :content
    (walk/postwalk 
      (fn [form]
        (or
          (when (and (vector? form) (map? (second form)) (some #{:src :href :srcset} (keys (second form))))
            (let [[tag attrs & content] form
                  attrs'  (-> attrs
                            (core/update-some :src absolutize (:uri page))
                            (core/update-some :href absolutize (:uri page))
                            (core/update-some :srcset absolutize-srcset (:uri page)))]
              (vec (concat [tag attrs'] content))))
          form))
      (:content page))))

(defn feed []
  (let [posts (->> (concat (index/old-posts) (index/new-posts))
                (remove index/draft?)
                (core/rsort-by :published)
                (take 5)
                (map absolutize-urls))]
    [:feed {:xmlns "http://www.w3.org/2005/Atom"
            :xml:lang "en-US"}
     [:title "tonsky.me"]
     [:subtitle "Nikita Prokopov’s blog"]
     [:link {:type "application/atom+xml"
             :href "https://tonsky.me/blog/atom.xml"
             :rel "self"}]
     [:link {:rel "alternate"
             :type "text/html"
             :href "https://tonsky.me/"}]
     [:id "https://tonsky.me/"]
     [:updated (as-> posts %
                 (mapcat (juxt :published :modified) %)
                 (reduce (core/max-by compare) %)
                 (core/format-temporal % date-format))]
     [:author
      [:name "Nikita Prokopov"]
      [:email "niki@tonsky.me"]]
     
     (for [post posts
           :let [url (str "https://tonsky.me" (:uri post))]]
       [:entry
        [:title (:title post)]
        [:link {:rel "alternate" :type "text/html" :href url}]
        [:id url]
        [:published (core/format-temporal (:published post) date-format)]
        [:updated (core/format-temporal (:modified post) date-format)]
        (when-some [summary (:summary post)]
          [:summary {:type "html"}
           [:CDATA summary]])
        [:content {:type "html"}
         [:CDATA (render/render-inner-html (:content post))]]
        [:author
         [:name "Nikita Prokopov"]
         [:email "niki@tonsky.me"]]])]))
