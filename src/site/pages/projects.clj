(ns site.pages.projects
  (:require
   [toml-clj.core :as toml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [site.cache :as cache]
   [site.core :as core]))

(def projects
  (core/memoize-by
    #(.lastModified (cache/touched (io/file "site/projects/projects.toml")))
    (fn []
      (->> (toml/read-string (slurp "site/projects/projects.toml") {:key-fn keyword})
        :project
        vec))))

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
    {:title  "Work"
     :uri    "/projects/"
     :styles ["/projects/projects.css"]
     :content
     [:.content
      [:h1 "Things I worked on"]
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
