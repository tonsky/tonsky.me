(ns site.pages.default
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [site.core :as core])
  (:import
    [java.net URLEncoder]))

(def about
  [:.about
   [:.about_photo]
   [:.about_inner
    [:p "Hi!"]
    [:p "I’m Niki. Here I write about programming and UI design "
     [:a.btn-subscribe {:href "/subscribe/"}
      [:img {:src "/i/subscribe.svg"}] " Subscribe"]]
    [:p "I also create open-source stuff: " [:a {:href "https://github.com/tonsky/FiraCode"} "Fira Code"] ", " [:a {:href "https://github.com/tonsky/DataScript"} "DataScript"] ", " [:a {:href "https://github.com/tonsky/Clojure-Sublimed"} "Clojure Sublimed"] " and " [:a {:href "https://github.com/HumbleUI/HumbleUI"} "Humble UI"] ". If you like what I do and want to get early access to my articles, you should " [:a {:href "https://patreon.com/tonsky"} "support me on Patreon"] "."]]])

(defn default [page]
  (let [url    (str "https://tonsky.me" (:uri page))
        long?  (> (count (:title page "")) 60)
        index? (:index (:categories page))
        post?  (:blog (:categories page))]
    (assoc page :content
      [:html {:lang "en", :prefix "og: http://ogp.me/ns#", :xmlns:og "http://opengraphprotocol.org/schema/"}
       [:head
        [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
        (if (= "page_wide" (:class page))
          [:meta {:name "viewport" :content "width=900"}]
          [:meta {:name "viewport" :content "width=640"}])
        [:meta {:name "theme-color" :content "#FDDB29"}]
        [:link {:href "/i/favicon.png" :rel "icon" :sizes "32x32"}]
        [:link {:href "/fonts/fonts.css" :rel "stylesheet" :type "text/css"}]
        [:link {:href "/style.css" :rel "stylesheet" :type "text/css"}]
        (for [style (:styles page)]
          [:link {:href style :rel "stylesheet" :type "text/css"}])
        [:title (:title page) " @ tonsky.me"]
        [:link {:href "/atom.xml" :rel "alternate" :title "Nikita Prokopov’s blog" :type "application/atom+xml"}]
        [:meta {:name "author" :content "Nikita Prokopov"}]
        [:meta {:property "og:title" :content (:title page)}]
        [:meta {:property "og:url" :content url}]
        [:meta {:name "twitter:title" :content (:title page)}]
        [:meta {:property "twitter:domain" :content "tonsky.me"}]
        [:meta {:property "twitter:url" :content url}]
        (when index?
          (list
            [:meta {:property "og:type" :content "website"}]
            [:meta {:property "og:image"
                    :content "https://dynogee.com/gen?id=24m2qx9uethuw6p&title=That+yellow+website"}]
            [:meta {:name "twitter:card" :content "summary_large_image"}]
            [:meta {:name "twitter:image"
                    :content "https://dynogee.com/gen?id=nm509093bpj50lv&title=That+yellow+website"}]))
        (when post?
          (list
            [:meta {:property "article:published_time" :content (:published page)}]
            
            [:meta {:property "og:type" :content "article"}]
            [:meta {:property "og:image"
                    :content (str "https://dynogee.com/gen"
                               "?id=" (if long? "niutqf5hxi7rjlw" "24m2qx9uethuw6p")
                               "&title=" (URLEncoder/encode (:title page)))}]
            [:meta {:name "twitter:card" :content "summary_large_image"}]
            [:meta {:name "twitter:image"
                    :content (str "https://dynogee.com/gen"
                               "?id=" (if long? "awabsokfn29i8xt" "nm509093bpj50lv")
                               "&title=" (URLEncoder/encode (:title page)))}]))
        (when-some [summary (:summary page)]
          (list
            [:meta {:property "og:description" :content summary}]
            [:meta {:name "twitter:description" :content summary}]))
        [:meta {:property "og:site_name" :content "tonsky.me"}]
        [:meta {:property "article:author" :content "https://www.facebook.com/nikitonsky"}]
        [:meta {:property "profile:first_name" :content "Nikita"}]
        [:meta {:property "profile:last_name" :content "Prokopov"}]
        [:meta {:property "profile:username" :content "tonsky"}]
        [:meta {:property "profile:gender" :content "male"}]
        [:meta {:name "twitter:creator" :content "@nikitonsky"}]
        [:script {:src "/script.js" :defer true :async true}]
        (for [script (:scripts page)]
          [:script {:src script :defer true :async true}])
        (when core/dev?
          [:script {:src "/watcher.js" :defer true :async true}])]
       [:body {:class (when (:dark page) "dark")}
        [:.page
         [:ul {:class "menu"}
          (for [[url title] [["/"          "Blog"]
                             ["/talks/"    "Talks"]
                             ["/projects/" "Projects"]
                             ["/design/"   "Logos"]
                             ["/patrons/"  "Patrons"]]]
            [:li {:class (cond
                           (= (:uri page) url) "selected"
                           (and post? (= url "/")) "inside")}
             [:a {:href url} title]])
          [:div {:class "spacer"}]
          [:div {:class "dark_mode"}
           [:#darkModeGlow]]]
      
         (when index?
           about)
      
         (:content page)
      
         (when post?
           about)]
      
        [:img#flashlight {:src "/i/flashlight.webp"}]
        [:.pointers]]])))
