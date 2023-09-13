(ns site.templates
  (:require
    [clojure.string :as str])
  (:import
    [java.time LocalDate]
    [java.time.format DateTimeFormatter]))

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
      [:link {:href "/style.css?v=20230913" :rel "stylesheet" :type "text/css"}]
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
       "function updateFlashlight(e) {
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

(defn parse-date [s format]
  (when s
    (LocalDate/parse
      s
      (DateTimeFormatter/ofPattern format))))

(defn format-date [^LocalDate date format]
  (when date
    (.format date
      (DateTimeFormatter/ofPattern format))))

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
                 (parse-date  "u-M-d")
                 (format-date "MMMM d, uuuu"))]
        
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
                [:a {:href (str "https://news.ycombinator.com/item?id=" hn) :target "_blank"} "HackerNews"])
              (when hn2
                [:a {:href (str "https://news.ycombinator.com/item?id=" hn2) :target "_blank"} "More HackerNews"])
              (when reddit
                [:a {:href reddit :target "_blank"} "Reddit"])
              (when reddit2
                [:a {:href reddit2 :target "_blank"} "More Reddit"]))))]
       
       [:script
        "document.querySelectorAll('.hoverable').forEach(function (e) {
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
