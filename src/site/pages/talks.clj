(ns site.pages.talks
  (:require
   [toml-clj.core :as toml]
   [clojure.java.io :as io]
   [clojure.math :as math]
   [clojure.string :as str]
   [clojure+.core :refer [cond+]]
   [ring.util.mime-type :as ring-mime]
   [site.core :as core]
   [site.render :as render])
  (:import
   [java.io File]
   [java.time LocalDate Period]))

(defn populate-versions [talk]
  (if-some [versions (:version talk)]
    (mapv #(merge (dissoc talk :version) %) versions)
    [(assoc talk :default true)]))

(defn set-lang [version]
  (if (some (set "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ") (str (:title version) (:desc version)))
    (assoc version :lang "RU")
    (assoc version :lang "EN")))

(defn set-id [version]
  (assoc version :id (core/format-temporal (:date version) "yyyy-MM-dd")))

(defn set-dates [version]
  (assoc version
    :published (.atStartOfDay ^LocalDate (:date version) core/UTC)
    :modified (.atStartOfDay ^LocalDate (:date version) core/UTC)))

(defn talk-cmp-key [talk]
  (reduce
    #(if (<= (compare %1 %2) 0) %1 %2)
    (map :date talk)))

(defn set-default [talk]
  (if (some :default talk)
    talk
    (let [key-fn  (fn [version]
                    [(some? (:content version))
                     (= "EN" (:lang version))
                     (:date version)])
          default (reduce (core/max-by #(compare (key-fn %1) (key-fn %2))) talk)]
      (mapv #(if (= % default) (assoc % :default true) %) talk))))

(def talks
  (core/memoize-by
    #(.lastModified (io/file "site/talks/talks.toml"))
    (fn []
      (-> "site/talks/talks.toml"
        slurp
        (toml/read-string {:key-fn keyword})
        :talk
        (->>
          (map populate-versions)
          (map #(mapv set-lang %))
          (map #(mapv set-id %))
          (map #(mapv set-dates %))
          (map set-default)
          (map #(sort-by :date core/reverse-compare %))
          (sort-by talk-cmp-key core/reverse-compare)
          (vec))))))

(defn slides-link [version]
  (let [{:keys [lang slides]} version]
    [:a {:class (if (re-matches #".*\.(pdf|md)" slides) "download" "popup")
         :href  (str "slides/" slides)}
     (if (= "RU" lang)
       "Слайды"
       "Slides")]))

(defn talk-view [version]
  (let [{:keys [event content thumb]} version
        [_ ext] (re-matches #".*\.([^.]+)" (str/lower-case (or content "")))]
    [:.talk-view
     (cond+
       (nil? content)
       [:.talk-view-stub
        (slides-link version)]

       (= "mp3" ext)
       [:.podcast {:background-image (str "covers/" event ".webp")}
        [:audio {:preload  "none"
                 :controls true}
         [:source
          {:src  (str "content/" content)
           :type "audio/mpeg"}]]]
       
       (#{"webm" "mp4"} ext)
       (let [mime (ring-mime/default-mime-types ext)]
         [:video
          {:poster   (str "content/" thumb)
           :preload  "none"
           :controls true}
          [:source
           {:src  (str "content/" content)
            :type mime}]])

       :else
       [:raw-html content])]))

(defn talk-details [version]
  (let [{:keys [url title desc content slides]} version]
    [:.talk-details
     [:h2 (if url [:a {:href url :target "_blank"} title] title)]
     [:p.talk-description [:raw-html desc]]
     (when (and slides content)
       [:p (slides-link version)])
     [:.spacer]]))

(defn talk [talk]
  [:.talk.talk_hidden
   (let [default (first (filter :default talk))]
     [:.talk-inner {:id (:id default)}
      [:.talk-view]
      (talk-details default)])
   (for [version talk
         :let [{:keys [default lang date event city]} version]]
     [:script
      {:type         "talk-version"
       :data-id      (:id version)
       :data-default (or default false)
       :data-lang    lang
       :data-date    (core/format-temporal date "MMMM d, yyyy")
       :data-event   (if city (str event ", " city) event)}
      (talk-view version)
      (talk-details version)])])

(defn page []
  (let [talks       (talks)
        versions    (mapcat identity talks)
        conferences (filter :country versions)
        podcasts    (remove :country versions)
        countries   (->> conferences
                      (map :country)
                      (frequencies)
                      (sort-by (juxt #(- (second %)) first)))]
    {:title   "Talks"
     :uri     "/talks/"
     :styles  ["/talks/talks.css"]
     :scripts ["/talks/talks.js"]
     :content
     [:.content
      [:h1 "Things I talked about"]
      [:ul
       [:li (str (count conferences) " talks at conferences in " (str/join ", " (map first countries)))]
       
       [:li (str (count podcasts) " podcasts appearances")]
       
       (let [[city cnt] (->> conferences
                          (map :city)
                          (frequencies)
                          (sort-by second core/reverse-compare)
                          first)]
         [:li "Most popular city: " city " " (core/pluralize cnt "({1} time)" "({1} times)")])
       
       (let [[conference cnt] (->> conferences
                                (map :event)
                                (frequencies)
                                (sort-by second core/reverse-compare)
                                first)]
         [:li "Most popular conference: " conference " " (core/pluralize cnt "({1} time)" "({1} times)")])
       
       (let [talk (->> talks
                    (sort-by count core/reverse-compare)
                    first)
             title (->> talk (filter :default) first :title)]
         [:li "Most popular talk: “" title "” " (core/pluralize (count talk) " ({1} repeat)" " ({1} repeats)")])
       
       (let [[podcast cnt] (->> podcasts
                             (map :event)
                             (frequencies)
                             (sort-by second core/reverse-compare)
                             first)]
         [:li "Most popular podcast: " podcast " " (core/pluralize cnt "({1} appearance)" "({1} appearances)")])
       
       (let [[year cnt] (->> versions
                          (map #(.getYear ^LocalDate (:date %)))
                          (frequencies)
                          (sort-by second core/reverse-compare)
                          first)]
         [:li "Busiest year: " year " (" (core/pluralize cnt "{1} event" "{1} events") ")"])
       
       (let [[month cnt] (->> versions
                           (map #(core/format-temporal (:date %) "MMMM yyyy"))
                           (frequencies)
                           (sort-by second core/reverse-compare)
                           first)]
         [:li "Busiest month: " month " (" (core/pluralize cnt "{1} event" "{1} events") ")"])
       
       (let [dates    (->> versions (map :date) sort distinct)
             pauses   (map #(.between java.time.temporal.ChronoUnit/DAYS %1 %2) dates (next dates))
             shortest (reduce min pauses)]
         [:li "Shortest period between talks: " (core/pluralize shortest "{1} day" "{1} days")])
       
       (let [dates   (->> versions (map :date) (cons (LocalDate/now)) sort distinct)
             pauses  (map #(.between java.time.temporal.ChronoUnit/DAYS %1 %2) dates (next dates))
             longest (reduce max pauses)]
         [:li "Longest period without talks: " (core/pluralize longest "{1} day" "{1} days")])
       
       (let [dates    (->> versions (map :date) sort distinct)
             pauses   (map #(.between java.time.temporal.ChronoUnit/DAYS %1 %2) dates (next dates))
             median   (math/round (core/median pauses))
             ; average  (/ (reduce + pauses) (count pauses))
             ]
         [:li "Median pause between talks: " (core/pluralize median "{1} day" "{1} days")])]
      [:.talks
       (map talk talks)]]}))

(defn render-atom [version]
  (let [{:keys [id lang title desc event content published modified slides thumb]} version
        type       (cond
                     (str/ends-with? content ".mp3")
                     "Podcast"
                     :else
                     "Talk")
        title      (str type ": " title " @ " event)
        url        (str "https://tonsky.me/talks/#" id)
        youtube-id (core/youtube-id content)]
    [:entry
     [:title title]
     [:link {:rel "alternate" :type "text/html" :href url}]
     [:id url]
     [:published (core/format-temporal published core/atom-date-format)]
     [:updated (core/format-temporal modified core/atom-date-format)]
     [:content {:type "html"}
      [:CDATA
       (render/render-inner-html
         (list
           [:p
            (cond
              (str/ends-with? content ".mp3")
              [:img {:src (str "https://tonsky.me/talks/covers/" event ".png")}
               [:audio {:controls true
                        :preload  "none"}
                [:source
                 {:src  (str "https://tonsky.me/talks/content/" content)
                  :type "audio/mpeg"}]]]
               
              (or
                (str/ends-with? content ".webm")
                (str/ends-with? content ".mp4"))
              [:video
               {:poster   (str "https://tonsky.me/talks/content/" thumb)
                :preload  "none"
                :controls true}
               [:source
                {:src  (str "https://tonsky.me/talks/content/" content)
                 :type (ring-mime/ext-mime-type content)}]])]

           [:p [:raw-html desc]]
           
           (when slides
             [:p [:a {:href (str "https://tonsky.me/talks/slides/" slides)}
                  (if (= "RU" lang)
                    "Слайды"
                    "Slides")]])))]]
     
     [:author
      [:name "Nikita Prokopov"]
      [:email "niki@tonsky.me"]]]))
