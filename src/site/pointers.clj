(ns site.pointers
  (:require
    [clojure.string :as str]
    [mount.core :as mount]
    [org.httpkit.server :as http]
    [site.core :as core]
    [site.pages.default :as default]
    [site.render :as render])
  (:import
    [java.time Instant]
    [java.time.format DateTimeFormatter]
    [java.util Timer TimerTask]))

(mount/defstate ^Timer timer
  :start (Timer. true)
  :stop  (.cancel ^Timer timer))

(defn- timer-task ^TimerTask [f]
  (proxy [TimerTask] []
    (run []
      (try
        (f)
        (catch Throwable t
          (.printStackTrace t))))))

(defn schedule
  ([f ^long delay]
   (let [t (timer-task f)]
     (.schedule timer t delay)
     t))
  ([f ^long delay ^long period]
   (let [t (timer-task f)]
     (.scheduleAtFixedRate timer t delay period)
     t)))

(def *clients
  (atom {}))

(def *pages
  (atom {}))

(def *dirty
  (atom #{}))

(defn handler [req]
  (let [{:strs [id page platform]} (:query-params req)
        id (parse-long id)]
    (http/as-channel req
      {:on-open
       (fn [ch]
         ; (prn "OPEN" id page)
         (swap! *clients assoc id {:ch ch, :page page, :platform platform, :joined (Instant/now)})
         (swap! *pages update page (fnil conj #{}) id))
       :on-close
       (fn [ch status]
         ; (prn "CLOSE" id page status)
         (swap! *clients dissoc id)
         (swap! *pages update page disj id))
       :on-receive
       (fn [ch msg]
         ; (prn "RCV" id page msg)
         (when-some [[_ x y] (re-find #"\[\s*(\d+),\s*(\d+)\s*\]" msg)]
           (swap! *clients update id assoc
             :x (parse-long x)
             :y (parse-long y)
             :updated (Instant/now))
           (swap! *dirty conj page)))})))

(defn stats [req]
  {:status 200
   :body
   (-> 
     {:title "Pointer stats"
      :content
      (list
        [:style
         #ml "table, th, td { border: 1px solid rgba(0,0,0,0.2); border-collapse: collapse; }
              .page { width: 100vw; }
              td, th { text-align: left; min-width: 100px; }"]
        [:table
         [:tr
          [:th "Pages"]
          [:td {:colspan 6} (str (count @*pages))]]
         (for [[page clients] @*pages]
           [:tr
            [:th page]
            [:td (str (count clients))]])
         [:tr
          [:th "Clients"]
          [:td {:colspan 6} (str (count @*clients))]]
         [:tr
          [:th "id"]
          [:th "x"]
          [:th "y"]
          [:th "platform"]
          [:th "joined"]
          [:th "updated"]]
         (for [[id client] @*clients]
           [:tr
            [:th (str id)]
            [:td (str (:x client))]
            [:td (str (:y client))]
            [:td (:platform client)]
            [:td (core/format-temporal (:joined client) "yyyy-MM-dd hh:mm:ss")]
            [:td (core/format-temporal (:updated client) "yyyy-MM-dd hh:mm:ss")]])])}
     default/default
     :content
     render/render-html)})

(def routes
  {"GET /pointers" handler
   "GET /pointers/stats" stats})

(defn page-msg [page]
  (let [ids    (get @*pages page)
        client-pos (fn [id]
                     (let [client (@*clients id)
                           {:keys [x y platform]} client]
                       (when (and id x y platform)
                         (str "[" id "," (:x client) "," (:y client) ",\"" platform "\"]"))))]
    (str "[" (str/join "," (keep client-pos ids)) "]")))

(defn broadcast []
  (let [[dirty _] (swap-vals! *dirty #{})]
    (doseq [page dirty
            :let [msg (page-msg page)]
            id (get @*pages page)
            :let [client (@*clients id)
                  ch (:ch client)]]
      (http/send! ch msg))))

(mount/defstate ^TimerTask broadcast-task
  :start
  (schedule broadcast 0 1000)
  :stop
  (.cancel ^TimerTask broadcast-task))
