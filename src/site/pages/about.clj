(ns site.pages.about
  (:require
   [site.core :as core])
  (:import
   [java.io File]
   [java.time Instant LocalDate ZoneId]))

(defn offset []
  (-> (ZoneId/of "Europe/Berlin")
    (.getRules)
    (.getOffset (Instant/now))
    str))

(defn page []
  {:title  "About"
   :uri    "/about/"
   :content
   [:.content.page_about
    [:figure [:img {:src "me.webp"}]]
    [:h1 "Hi there!"]
    [:p "I’m Nikita Prokopov (aka Niki, Nikitonsky, Tonsky; please use full name for citations), Software Engineer with a vast open-source portfolio and strong UI/UX background."]
      
    [:p "I do consulting work on all matters Clojure/Script: JVM, web, backend, Datomic, performance, custom OSS modifications, etc."]

    [:dl
     [:dt "Expertise"]
     [:dd (- (.getYear (LocalDate/now core/UTC)) 2005) " years of distributed systems, highly interactive web apps (full stack), UX/UI design, Clojure/Script, Erlang, Python, Kotlin, Java"]
     [:dt "Github"]
     [:dd [:a {:href "https://github.com/tonsky"} "github.com/tonsky"]]
     [:dt "Social"]
     [:dd [:a {:href "https://mastodon.online/@nikitonsky"} "mastodon.online/@nikitonsky"]]
     [:dt "Contact me"]
     [:dd [:a {:href "mailto:niki@tonsky.me"} "niki@tonsky.me"] " or " [:a {:href "https://t.me/nikitonsky"} "t.me/nikitonsky"]]
     [:dt "Location"]
     [:dd "Berlin, Germany (GMT" (offset) ")"]]]})
