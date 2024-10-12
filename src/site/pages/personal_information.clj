(ns site.pages.personal-information
  (:require
   [clojure.string :as str]
   [ring.middleware.cookies :as ring-cookies]
   [site.core :as core]
   [site.render :as render]))

(defn page []
  {:title  "Personal Information"
   :uri    "/personal-information/"
   :styles
   [".outer { display: flex; flex-direction: column; align-items: flex-start; gap: var(--gap); }
     .outer > p, textarea { width: 96vw; max-width: var(--width); }
     textarea { display: block; field-sizing: content; min-height: 10em; border: 1.5px solid transparent; background: #FFFFFF60; border-radius: 6px; margin: 0 -1em 0.5em; padding: 0.5em 1em; font: inherit; }
     textarea:disabled { background: #00000020; color: #00000030; }
     textarea:focus { outline: none; background: #FFFFFF80; }
     .gap { height: 3em; }"]
   :content
   [:.outer
    [:script
     [:raw-html
      "function onSubmit(button) {
         let ta = document.querySelector('textarea');
         if (ta.value.trim() == '')
           return;
         ta.disabled = true;
         button.disabled = true;
         button.innerHTML = '<img src=\"/i/spinner.svg\"> Storing...';
         setTimeout(() => {
           fetch('/personal-information/submit/', {
             method: 'POST',
             body: ta.value
           }).then(resp => {
             let p = document.createElement('p');
             p.style.fontWeight = 'bold';
             p.textContent = 'Personal Information stored successfully!';
             let parent = ta.parentNode;
             parent.insertBefore(p, ta);
             parent.removeChild(ta);
             parent.removeChild(button);
           });
         }, 2000);
       }"]]
    [:h1 "Personal Information"]
    [:p "By default, tonsky.me does not store any personal information."]
    [:p "BUT"]
    [:p "If you want to, we can store your personal information for you."]
    [:p "Enter your personal information:"]
    [:textarea {:maxlength 1000}]
    [:button {:onClick "onSubmit(this)"} "Store on tonsky.me"]
    [:h1 "Download"]
    [:p "You can also download personal information: "]
    [:p [:a {:href "/personal-information.txt" :download true} "Download"]]]})

(def ^java.util.concurrent.locks.ReentrantReadWriteLock lock
  (java.util.concurrent.locks.ReentrantReadWriteLock.))

(defn submit [req]
  (-> lock .writeLock .lock)
  (try
    (let [time (-> (java.time.LocalDateTime/now)
                 (.atZone java.time.ZoneOffset/UTC)
                 (->> (.format java.time.format.DateTimeFormatter/RFC_1123_DATE_TIME)))
          auth (-> req :cookies (get "signed_in") :value (= "true"))
          pi   (-> (:body req) slurp str/trim)
          pi   (subs pi 0 (min (count pi) 1000))
          text (str
                 "Timestamp: " time "\n"
                 (when auth "Signed In: true\n")
                 "\n"
                 pi
                 "\n\n--------------------------------------------------------------------------------\n\n")]
      (spit "files/personal-information.txt" text :append true)
      {:status 200
       :body "success"})
    (finally
      (-> lock .writeLock .unlock))))
