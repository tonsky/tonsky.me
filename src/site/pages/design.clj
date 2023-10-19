(ns site.pages.design
  (:require
    [toml-clj.core :as toml]
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [site.cache :as cache]
    [site.core :as core])
  (:import
    [java.io File]
    [java.time LocalDate]))

(defn set-year [design]
  (assoc design :year
    (if-some [date (:date design)]
      (.getYear ^LocalDate date)
      (when-some [[_ year] (re-matches #"(\d{4})-.*" (:img design))]
        (parse-long year)))))

(defn set-slug [design]
  (assoc design :slug
    (let [[_ slug] (re-matches #"(.*)\.\w+" (:img design))]
      slug)))

(def designs
  (core/memoize-by
    #(.lastModified (cache/touched (io/file "site/design/design.toml")))
    (fn []
      (-> "site/design/design.toml"
        slurp
        (toml/read-string {:key-fn keyword})
        :design
        (->>
          (map set-year)
          (map set-slug)
          vec)))))

(defn page []
  (let [designs (designs)]
    {:title  "Logos"
     :uri    "/design/"
     :styles ["/design/design.css"]
     :content
     [:.content
      (for [[year designs] (group-by :year designs)]
        (list
          [:h1 year]
          (for [design (sort-by :img core/reverse-compare designs)]
            (list
              [:figure
               (core/some-map
                 :id    (:slug design)
                 :class (:class design))
               [:img {:src (str "images/" (:img design))}]]
              [:p [:raw-html (:desc design)]]))))]}))
