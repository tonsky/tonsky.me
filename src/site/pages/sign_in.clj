(ns site.pages.sign-in
  (:require
   [ring.middleware.cookies :as ring-cookies]
   [site.core :as core]
   [site.render :as render]))

(defn page [req]
  {:title  "Sign In"
   :uri    "/sign-in/"
   :styles ["/sign-in.css"]
   :content
   [:.content
    [:script
     [:raw-html
      "function onClick() {
        let b = document.querySelector('.content button');
        b.disabled = true;
        if (b.innerText == 'Sign In') {
          b.innerHTML = '<img src=\"/i/spinner.svg\"> Signing in...';
          document.cookie = 'signed_in=true; expires=Fri, 31 Dec 9999 23:59:59 GMT; path=/';
        } else {
          b.innerHTML = '<img src=\"/i/spinner.svg\"> Signing out...';
          document.cookie = 'signed_in=false; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
        }
        setTimeout(() => { location.reload() }, 2000);
      }"]]
    (if (= "true" (-> req :cookies (get "signed_in") :value))
      (list
        [:.message "You are now signed in"]
        [:button {:onClick "onClick()"}
         "Sign Out"])
      [:button {:onClick "onClick()"}
       "Sign In"])]})