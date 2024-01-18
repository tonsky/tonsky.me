(ns site.pages.design
  (:require
    [toml-clj.core :as toml]
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [site.cache :as cache]
    [site.core :as core]
    [site.render :as render])
  (:import
    [java.io File]
    [java.time LocalDate]))

(defn set-year [design]
  (assoc design :year
    (if-some [date (:date design)]
      (.getYear ^LocalDate date)
      (when-some [[_ year] (re-matches #"(\d{4})-.*" (:img design))]
        (parse-long year)))))

(defn set-dates [version]
  (cond-> version
    (:date version)
    (assoc
      :published (.atStartOfDay ^LocalDate (:date version) core/UTC)
      :modified (.atStartOfDay ^LocalDate (:date version) core/UTC))))

(defn set-slug [design]
  (assoc design :slug
    (let [[_ slug] (re-matches #"(.*)\.\w+" (:img design))]
      slug)))

(defn set-title [design]
  (if-some [[_ title] (re-matches #".*<a [^>]+>([\w\d\s]+)</a>.*" (:desc design))]
    (assoc design :title title)
    design))

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
          (map set-dates)
          (map set-slug)
          (map set-title)
          vec)))))

(defn page []
  (let [designs (designs)]
    {:title  "Logos"
     :uri    "/design/"
     :styles ["/design/design.css"]
     :content
     [:.content
      (for [[year designs] (->> (group-by :year designs)
                             (core/rsort-by first))]
        (list
          [:h1 year]
          (for [design (core/rsort-by :date designs)]
            (list
              [:figure
               (core/some-map
                 :id    (:slug design)
                 :class (:class design))
               [:img {:src (str "images/" (:img design))}]]
              [:p [:raw-html (:desc design)]]))))]}))

(defn render-atom [design]
  (let [url   (str "https://tonsky.me/design/#" (:slug design))
        file  (cache/find-file nil (str "/design/images/" (:img design)))
        [w h] (core/image-dimensions file)]
    [:entry
     [:title "Logo: " (:title design)]
     [:link {:rel "alternate" :type "text/html" :href url}]
     [:id url]
     [:published (core/format-temporal (:published design) core/atom-date-format)]
     [:updated (core/format-temporal (:modified design) core/atom-date-format)]
     [:content {:type "html"}
      [:CDATA
       (render/render-inner-html
         (list
           [:img {:src    (str "https://tonsky.me/design/images/" (:img design))
                  :style  (str "aspect-ratio: " w "/" h "; ")
                  :width  1000
                  :height (-> h (/ w) (* 1000) int)}]
           [:br]
           [:raw-html (:desc design)]))]]
     [:author
      [:name "Nikita Prokopov"]
      [:email "niki@tonsky.me"]]]))
