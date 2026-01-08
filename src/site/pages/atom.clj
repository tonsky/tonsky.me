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

(defn feed [req]
  (let [all      (->> (concat (posts) (talks) (designs))
                   (sort-by :published))
        pages    (partition-all 5 all)
        max-page (- (count pages) 2)
        page     (some-> req :query-params (get "page") parse-long)
        entries  (if page
                   (nth pages (dec page))
                   (apply concat (take-last 2 pages)))]
    [:feed {:xmlns       "http://www.w3.org/2005/Atom"
            :xml:lang    "en-US"
            :xmlns:yt    "http://www.youtube.com/xml/schemas/2015"
            :xmlns:media "http://search.yahoo.com/mrss/"}
     [:title "tonsky.me"]
     [:subtitle "Nikita Prokopov’s blog"]
     [:link {:rel "self"
             :href "https://tonsky.me/atom.xml"
             :type "application/atom+xml"}]
     [:link {:rel "alternate"
             :href "https://tonsky.me/"
             :type "text/html"}]
     (cond
       (nil? page) [:link {:rel "next" :href (str "https://tonsky.me/atom.xml?page=" max-page)}]
       (> page 1)  [:link {:rel "next" :href (str "https://tonsky.me/atom.xml?page=" (dec page))}])

     (cond
       (nil? page)       nil
       (< page max-page) [:link {:rel "prev" :href (str "https://tonsky.me/atom.xml?page=" (inc page))}]
       (= page max-page) [:link {:rel "prev" :href (str "https://tonsky.me/atom.xml")}])

     [:id "https://tonsky.me/"]
     [:updated (as-> entries %
                 (mapcat (juxt :published :modified) %)
                 (reduce (core/max-by compare) %)
                 (core/format-temporal % core/atom-date-format))]
     [:author
      [:name "Nikita Prokopov"]
      [:email "niki@tonsky.me"]]
     
     (for [entry (reverse entries)]
       (case (:type entry)
         :post   (post/render-atom entry)
         :talk   (talks/render-atom entry)
         :design (design/render-atom entry)))]))
