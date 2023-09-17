(ns site.pages.default
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [site.core :as core]))

(defn default [page]
  (assoc page :content
    [:html {:lang "en", :prefix "og: http://ogp.me/ns#", :xmlns:og "http://opengraphprotocol.org/schema/"}
     [:head
      [:meta {:http-equiv "content-type" :content "text/html;charset=UTF-8"}]
      (if (= "page_wide" (:class page))
        [:meta {:name "viewport" :content "width=900"}]
        [:meta {:name "viewport" :content "width=640"}])
      [:meta {:name "theme-color" :content "#FDDB29"}]
      [:link {:href "/i/favicon.png" :rel "icon" :sizes "32x32"}]
      [:link {:href (core/timestamp-url "/style.css" "site/style.css") :rel "stylesheet" :type "text/css"}]
      [:title (:title page) " @ tonsky.me"]
      [:link {:href "/blog/atom.xml" :rel "alternate" :title "Nikita Prokopov’s blog" :type "application/atom+xml"}]
      [:meta {:name "author" :content "Nikita Prokopov"}]
      [:meta {:property "og:title" :content (:title page)}]
      [:meta {:property "og:url" :content (str "https://tonsky.me" (:url page))}]
      (when (:blog (:categories page))
        (list
          [:meta {:property "og:type" :content "article"}]
          [:meta {:property "article:published_time" :content (:published page)}]
          [:meta {:name "twitter:card" :content "summary"}]
          [:meta {:name "twitter:title" :content (:title page)}]))
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
      [:meta {:name "twitter:creator" :content "@nikitonsky"}]]
     [:body
      [:div {:class "page"}
       (:content page)
       [:div {:class "preload"}]]
      [:script
       #ml "function updateFlashlight(e) {
              var style = document.body.style;
              style.backgroundPositionX = e.pageX - 250 + 'px';
              style.backgroundPositionY = e.pageY - 250 + 'px';
            }
            
            document.querySelector('.dark_mode').onclick = function(e) {
              var body = document.body;
              body.classList.toggle('dark');
              if (body.classList.contains('dark')) {
                updateFlashlight(e);
                ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
                  document.documentElement.addEventListener(s, updateFlashlight, false);
                });
              } else {
                ['mousemove', 'touchstart', 'touchmove', 'touchend'].forEach(function(s) {
                  document.documentElement.removeEventListener(s, updateFlashlight, false);
                });
              }
            }"]]]))

