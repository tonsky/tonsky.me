(ns site.pages.index
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [site.core :as core]
    [site.parser :as parser])
  (:import
    [java.io File]
    [java.time Instant LocalDate ZonedDateTime]))

(def list-files
  (core/memoize-by
    #(.lastModified (io/file %))
    #(.listFiles (io/file %))))

(defn old-posts []
  (for [^File file (list-files "_site/_posts")
        :let  [name (.getName file)
               [_ date slug] (re-matches #"(\d{4}-\d{2}-\d{2})-(.+)\.md" name)]
        :when (and date slug)
        :let  [parsed (parser/parse-md file)]]
    (assoc parsed
      :published (when-not (:draft parsed)
                   (-> ^LocalDate (core/parse-date date "u-M-d")
                     (.atStartOfDay core/UTC)))
      :modified  (-> (Instant/ofEpochMilli (.lastModified file))
                   (.atZone core/UTC))
      :uri       (str "/blog/" slug "/"))))

(defn new-posts []
  (for [^File dir (list-files "site/blog")
        :when (.isDirectory dir)
        :let [file   (io/file dir "index.md")
              parsed (parser/parse-md file)]]
    (assoc parsed
      :published (some-> (:published parsed)
                   (core/parse-date "u-M-d")
                   (.atStartOfDay core/UTC))
      :modified  (-> (Instant/ofEpochMilli (.lastModified file))
                   (.atZone core/UTC))
      :uri       (str "/blog/" (.getName dir) "/"))))

(defn draft? [post]
  (or
    (nil? (:published post))
    (let [now (ZonedDateTime/now core/UTC)]
      (pos? (compare (:published post) now)))))

(defn index []
  (let [posts     (concat (old-posts) (new-posts))
        drafts    (filterv draft? posts)
        published (->> posts
                    (remove draft?)
                    (group-by #(.getYear ^ZonedDateTime (:published %)))
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
                  (core/format-temporal published "M/d")])])))
       
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
                (core/format-temporal (:published post) "M/d")]])))
        ])}))
