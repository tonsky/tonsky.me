(ns site.pages.user-agreement
  (:require
   [ring.middleware.cookies :as ring-cookies]
   [site.core :as core]
   [site.render :as render]))

(defn page []
  {:title "User Agreement"
   :uri "/user-agreement/"
   :styles
   [".outer {flex-grow: 1; display: flex; flex-direction: column; justify-content: center; gap: var(--gap); }"]
   :content
   [:.outer
    [:h1 "USER AGREEMENT"]
    [:p "User disagrees with the content of this site."]]})