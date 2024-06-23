(ns site.pages.projects
  (:require
    [toml-clj.core :as toml]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [site.cache :as cache]
    [site.core :as core])
  (:import
    [java.io File]
    [java.time Instant LocalDate ZonedDateTime ZoneId]))

(def projects
  (core/memoize-by
    #(.lastModified (cache/touched (io/file "site/projects/projects.toml")))
    (fn []
      (->> (toml/read-string (slurp "site/projects/projects.toml") {:key-fn keyword})
        :project
        vec))))

(defn offset []
  (-> (ZoneId/of "Europe/Berlin")
    (.getRules)
    (.getOffset (Instant/now))
    str))

(defn round [x]
  (cond
    (>= x 2000)
    (str (quot x 1000) "K+")
    
    (>= x 1000)
    (let [format (java.text.DecimalFormat. "0.#" (java.text.DecimalFormatSymbols. java.util.Locale/ENGLISH))]
      (str (.format format (/ x 1000)) "K+"))

    (>= x 100)
    (str (-> x (quot 100) (* 100)) "+")
    
    :else 
    x))

(defn page []
  (let [projects (projects)]
    {:title  "Projects"
     :uri    "/projects/"
     :styles ["/projects/projects.css"]
     :content
     [:.content
      [:figure [:img {:src "images/me.webp"}]]
      [:h1 "Hi there!"]
      [:p "I’m Niki, Software Engineer with a vast open-source portfolio and strong UI/UX background."]
      
      [:p "I do consulting work on all matters Clojure/Script: JVM, web, backend, Datomic, performance, custom OSS modifications, etc."]

      [:dl
       [:dt "Expertise"]
       [:dd (- (.getYear (LocalDate/now core/UTC)) 2005) " years of distributed systems, highly interactive web apps (full stack), UX/UI design, Clojure/Script, Erlang, Python, Kotlin, Java"]
       [:dt "Github"]
       [:dd [:a {:href "https://github.com/tonsky"} "github.com/tonsky"]]
       [:dt "Contact me"]
       [:dd [:a {:href "mailto:niki@tonsky.me"} "niki@tonsky.me"] " or @" [:a {:href "https://t.me/nikitonsky"} "nikitonsky"]]
       [:dt "Location"]
       [:dd "Berlin, Germany (GMT" (offset) ")"]]

      [:h1 "What I worked on (chronological order)"]
      [:.projects
       (for [{:keys [link img name desc roles team period customer status stack stars installs visitors used-by quote]} projects]
         (list
           [:.project
            [:.project-img
             (when img
               (if link
                 [:a {:href link}
                  [:img {:src (str "images/" img)}]]
                 [:img {:src (str "images/" img)}]))]
            [:.project-details
             [:h2
              (if link [:a {:href link} name] name)
              (when stars
                [:span.stars {:title "GitHub Stars"} (round stars)])
              (when installs
                [:span.installs {:title "Installs"} (round installs)])
              (when visitors
                [:span.visitors {:title "Visitors"} (round visitors) "/mo"])]
             [:p [:raw-html desc]]
             [:dl
              (when roles
                (list [:dt (core/pluralize (count roles) "Role" "Roles")] [:dd (str/join ", " roles)]))
              (when team
                (list [:dt "Team"] [:dd team]))
              (when period
                (list [:dt "Period"] [:dd period]))
              (when customer
                (list [:dt "Customer"] [:dd [:raw-html customer]]))
              (when status
                (list [:dt "Status"] [:dd status]))
              (when stack
                (list [:dt "Stack"] [:dd [:raw-html stack]]))
              (when used-by
                (list [:dt "Used by"] [:dd [:raw-html (str/join ", " used-by)]]))]]]
           
           (for [quote quote
                 :let [{:keys [text source]} quote]]
             [:blockquote
              [:p [:raw-html text]]
              [:p.source "— " [:raw-html source]]])
           
           ))]]}))
