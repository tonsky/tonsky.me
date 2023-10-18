(ns site.pages.post
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [site.core :as core]
    [site.render :as render])
  (:import
    [java.io File]))

(defn post [page]
  (assoc page :content
    [:article.content
     ;; title
     [:h1 {:class "title"} (:title page)]
     
     ;; content
     (:content page)
     
     ;; footer
     [:p {:class "footer"}
        
      ;; date
      [:span (-> (:published page)
               (core/parse-date  "u-M-d")
               (core/format-temporal "MMMM d, uuuu"))]
        
      ;; related
      (when-some [related-url (:related_url page)]
        (when-some [related-title (:related_title page)]
          (list
            [:span {:class "separator"} "·"]
            [:span "Related"]
            [:a {:href related-url} related-title])))
        
      ;; comments
      (let [{hn      :hackernews_id
             hn2     :hackernews_id_2
             reddit  :reddit_url
             reddit2 :reddit_url_2} page]
        (when (or hn hn2 reddit reddit)
          (list
            [:span {:class "separator"} "·"]
            [:span "Discuss on"]
            (when hn
              (list " " [:a {:href (str "https://news.ycombinator.com/item?id=" hn)} "HackerNews"]))
            (when hn2
              (list " " [:a {:href (str "https://news.ycombinator.com/item?id=" hn2)} "More HackerNews"]))
            (when reddit
              (list " " [:a {:href reddit} "Reddit"]))
            (when reddit2
              (list " " [:a {:href reddit2} "More Reddit"])))))]]))

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

(defn render-atom [post]
  (let [post (absolutize-urls post)
        url  (str "https://tonsky.me" (:uri post))]
    [:entry
     [:title (:title post)]
     [:link {:rel "alternate" :type "text/html" :href url}]
     [:id url]
     [:published (core/format-temporal (:published post) core/atom-date-format)]
     [:updated (core/format-temporal (:modified post) core/atom-date-format)]
     (when-some [summary (:summary post)]
       [:summary {:type "html"}
        [:CDATA summary]])
     [:content {:type "html"}
      [:CDATA (render/render-inner-html (:content post))]]
     [:author
      [:name "Nikita Prokopov"]
      [:email "niki@tonsky.me"]]]))
