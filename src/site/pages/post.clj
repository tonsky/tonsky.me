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
    (list
      [:ul {:class "menu"}
       [:li [:a {:class "menu__item menu__item_inside" :href "/"} "Blog"]]
       [:li [:a {:class "menu__item" :href "/talks/"} "Talks"]]
       [:li [:a {:class "menu__item" :href "/projects/"} "Projects"]]
       [:li [:a {:class "menu__item" :href "/design/"} "Logos"]]
       [:li [:a {:class "menu__item" :href "/patrons/"} "Patrons"]]
       [:div {:class "spacer"}]
       [:div {:class "dark_mode"}]]

      [:article {:class "post"}
       
       [:h1 {:class "title"} (:title page)]
       
       (:content page)
       
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
                (list " " [:a {:href (str "https://news.ycombinator.com/item?id=" hn) :target "_blank"} "HackerNews"]))
              (when hn2
                (list " " [:a {:href (str "https://news.ycombinator.com/item?id=" hn2) :target "_blank"} "More HackerNews"]))
              (when reddit
                (list " " [:a {:href reddit :target "_blank"} "Reddit"]))
              (when reddit2
                (list " " [:a {:href reddit2 :target "_blank"} "More Reddit"])))))]
       
       [:script
        #ml "document.querySelectorAll('.hoverable').forEach(function (e) {
               e.onclick = function() { e.classList.toggle('clicked'); }
             });"]
       
       [:div {:class "about"}
        [:div {:class "about_photo"}]
        [:div {:class "about_inner"}
         [:p "Hi!"]
         [:p "I’m Nikita. Here I write about programming and UI design "
          [:raw-html "<a style='margin-left: 5px' class='btn-subscribe' href='/blog/how-to-subscribe/' target='_blank'><svg viewBox='0 0 800 800'><path d='M493 652H392c0-134-111-244-244-244V307c189 0 345 156 345 345zm71 0c0-228-188-416-416-416V132c285 0 520 235 520 520z'/><circle cx='219' cy='581' r='71'/></svg> Subscribe</a>"]]
         [:p "I also create open-source stuff: Fira Code, AnyBar, DataScript and Rum. If you like what I do and want to get early access to my articles (along with other benefits), you should "
          [:raw-html "<a href='https://patreon.com/tonsky' target='_blank'>support me on Patreon</a>"]
          "."]]]])))

