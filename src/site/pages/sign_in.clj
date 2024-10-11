(ns site.pages.sign-in
  (:require
   [ring.middleware.cookies :as ring-cookies]
   [site.core :as core]
   [site.render :as render]))

(defn page [req]
  {:title "Sign In"
   :uri "/sign-in/"
   :styles
   [".outer {flex-grow: 1; display: flex; flex-direction: column; justify-content: center; }"]
   :content
   [:.outer
    [:script
     [:raw-html
      "function onClick(button) {
        onButtonClick(button, 'Signing in...', () => { 
          document.cookie = 'signed_in=true; expires=Fri, 31 Dec 9999 23:59:59 GMT; path=/';
          location.reload();
        });
      }"]]
    (if (= "true" (-> req :cookies (get "signed_in") :value))
      [:.message "You are signed in"]
      [:button {:onClick "onClick(this)"}
       "Sign In"])]})