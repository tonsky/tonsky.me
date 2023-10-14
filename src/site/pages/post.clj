(ns site.pages.post
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [site.core :as core])
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
