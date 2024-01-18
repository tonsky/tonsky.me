(ns site.pages.atom
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [site.core :as core]
    [site.pages.design :as design]
    [site.pages.index :as index]
    [site.pages.post :as post]
    [site.pages.talks :as talks]
    [site.parser :as parser])
  (:import
    [java.io File]
    [java.time LocalDate]))

(defn posts []
  (->> (index/posts)
    (remove index/draft?)
    (map #(assoc % :type :post))))

(defn talks []
  (->> (talks/talks)
    (mapcat identity)
    (map #(assoc % :type :talk))))

(defn designs []
  (->> (design/designs)
    (filter :date)
    (map #(assoc % :type :design))))

(defn feed []
  (let [entries (->> (concat (posts) (talks) (designs))
                  (core/rsort-by :published)
                  (take 5))]
    [:feed {:xmlns       "http://www.w3.org/2005/Atom"
            :xml:lang    "en-US"
            :xmlns:yt    "http://www.youtube.com/xml/schemas/2015"
            :xmlns:media "http://search.yahoo.com/mrss/"}
     [:title "tonsky.me"]
     [:subtitle "Nikita Prokopov’s blog"]
     [:link {:type "application/atom+xml"
             :href "https://tonsky.me/atom.xml"
             :rel "self"}]
     [:link {:rel "alternate"
             :type "text/html"
             :href "https://tonsky.me/"}]
     [:id "https://tonsky.me/"]
     [:updated (as-> entries %
                 (mapcat (juxt :published :modified) %)
                 (reduce (core/max-by compare) %)
                 (core/format-temporal % core/atom-date-format))]
     [:author
      [:name "Nikita Prokopov"]
      [:email "niki@tonsky.me"]]
     
     (for [entry entries]
       (case (:type entry)
         :post   (post/render-atom entry)
         :talk   (talks/render-atom entry)
         :design (design/render-atom entry)))]))
