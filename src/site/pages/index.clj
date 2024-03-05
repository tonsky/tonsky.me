(ns site.pages.index
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [site.core :as core]
    [site.parser :as parser])
  (:import
    [java.io File]
    [java.time Instant LocalDate ZonedDateTime]))

(defn posts []
  (for [^File dir (core/list-files "site/blog")
        :when (.isDirectory dir)
        :let [file (io/file dir "index.md")]
        :when (.exists file)
        :let [uri    (str "/blog/" (.getName dir) "/")
              parsed (parser/parse-md uri)]]
    (assoc parsed
      :published (some-> (:published parsed)
                   (core/parse-date "u-M-d")
                   (.atStartOfDay core/UTC))
      :modified  (-> (Instant/ofEpochMilli (.lastModified file))
                   (.atZone core/UTC)))))

(defn draft? [post]
  (or
    (nil? (:published post))
    (let [now (ZonedDateTime/now core/UTC)]
      (pos? (compare (:published post) now)))))

(defn index []
  (let [posts     (posts)
        drafts    (filterv draft? posts)
        published (->> posts
                    (remove draft?)
                    (group-by #(.getYear ^ZonedDateTime (:published %)))
                    (core/rsort-by first)
                    (mapv (fn [[year posts]]
                            [year (core/rsort-by :published posts)])))]
    {:title "Blog"
     :uri   "/"
     :categories #{:index}
     :content
     [:.content
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
              (core/format-temporal (:published post) "M/d")]])))]}))
