(ns site.templates
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [site.core :as core]
    [site.parser :as parser])
  (:import
    [java.io File]
    [java.time LocalDate ZoneId]))

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

(def list-files
  (core/memoize-by
    #(.lastModified (io/file %))
    #(.listFiles (io/file %))))

(def parsed-md
  (core/memoize-by
    #(.lastModified (io/file %))
    #(-> (io/file %)
       slurp
       parser/parse
       parser/transform)))

(defn old-posts []
  (for [^File file (list-files "_site/_posts")
        :let  [name (.getName file)
               [_ date slug] (re-matches #"(\d{4}-\d{2}-\d{2})-(.+)\.md" name)]
        :when (and date slug)
        :let  [parsed (parsed-md file)]]
    {:title     (:title parsed)
     :published (when-not (:draft parsed)
                  (core/parse-date date "u-M-d"))
     :uri       (str "/blog/" slug "/")
     :starred   (:starred parsed)}))

(defn new-posts []
  (for [^File dir (list-files "site/blog")
        :when (.isDirectory dir)
        :let [parsed (parsed-md (io/file dir "index.md"))]]
    {:title     (:title parsed)
     :published (core/parse-date (:published parsed) "u-M-d")
     :uri       (str "/blog/" (.getName dir) "/")
     :starred   (:starred parsed)}))

(defn index []
  (let [posts     (concat (old-posts) (new-posts))
        today     (LocalDate/now (ZoneId/of "UTC"))
        draft?    #(or
                     (nil? (:published %))
                     (pos? (compare (:published %) today)))
        drafts    (filterv draft? posts)
        published (->> posts
                    (remove draft?)
                    (group-by #(.getYear ^LocalDate (:published %)))
                    (core/rsort-by first)
                    (mapv (fn [[year posts]]
                            [year (core/rsort-by :published posts)])))]
    {:title "Blog"
     :url   "/"
     :content
     (list
       ;; menu
       [:ul {:class "menu"}
        [:li [:a {:class "menu__item menu__item_selected" :href "/"} "Blog"]]
        [:li [:a {:class "menu__item" :href "/talks/"} "Talks"]]
        [:li [:a {:class "menu__item" :href "/projects/"} "Projects"]]
        [:li [:a {:class "menu__item" :href "/design/"} "Logos"]]
        [:li [:a {:class "menu__item" :href "/patrons/"} "Patrons"]]
        [:div {:class "spacer"}]
        [:div {:class "dark_mode"}]]
       
       [:div {:class "post"}
        
        ;; about
        [:div {:class "about"}
         [:div {:class "about_photo"}]
         [:div {:class "about_inner"}
          [:p "Hi!"]
          [:p "I’m Nikita. Here I write about programming and UI design "
           [:raw-html "<a style='margin-left: 5px' class='btn-subscribe' href='/blog/how-to-subscribe/' target='_blank'><svg viewBox='0 0 800 800'><path d='M493 652H392c0-134-111-244-244-244V307c189 0 345 156 345 345zm71 0c0-228-188-416-416-416V132c285 0 520 235 520 520z'/><circle cx='219' cy='581' r='71'/></svg> Subscribe</a>"]]
          [:p "I also create open-source stuff: Fira Code, AnyBar, DataScript and Rum. If you like what I do and want to get early access to my articles (along with other benefits), you should "
           [:raw-html "<a href='https://patreon.com/tonsky' target='_blank'>support me on Patreon</a>"]
           "."]]]
        
        ;; drafts
        (when (and core/dev? (not-empty drafts))
          (list
            [:h1 "Drafts"]
            (for [post drafts]
              [:p
               [:a {:href (:uri post)} (:title post)]
               (when-some [published (:published post)]
                 [:span {:class "date"}
                   (core/format-date published "M/d")])])))
       
        ;; posts
        (for [[year posts] published]
          (list
            [:h1 (str year)]
            (for [post posts]
              [:p
               (when (:starred post)
                 [:span {:class "starred"} "★"])
               [:a {:href (:uri post)} (:title post)]
               [:span {:class "date"}
                (core/format-date (:published post) "M/d")]])))
        ])}))

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
                 (core/format-date "MMMM d, uuuu"))]
        
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

(def image-dimensions
  (core/memoize-by
    #(.lastModified ^File %)
    (fn [^File file]
      (-> (core/sh "convert" (.getPath file) "-ping" "-format" "[%w,%h]" "info:")
        :out
        edn/read-string))))

(defn add-image-dimensions [page dir]
  (update page :content
    #(walk/postwalk 
       (fn [form]
         (or
           (when (and (vector? form) (= :img (first form)))
             (let [attrs (second form)
                   src   (:src attrs)
                   file  (io/file dir src)]
               (when (.exists file)
                 (let [modified (.lastModified file)
                       [w h]    (image-dimensions file)]
                   [:img (assoc attrs
                           :src    (str src "?modified=" modified)
                           :width  w
                           :height h)]))))
           form))
       %)))
