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
     (.schedule timer t delay period)
     t)))

(def page-limit
  20)

(def *pages
  (atom {}))

(def *dirty
  (atom #{}))

(defn find-page [page]
  (loop [i 0]
    (let [id (str page i)]
      (if (>= (count (get @*pages id)) page-limit)
        (recur (inc i))
        id))))

(defn handler [req]
  (let [{:strs [id page platform]} (:query-params req)
        page (find-page page)
        id   (parse-long id)]
    (http/as-channel req
      {:on-open
       (fn [ch]
         ; (prn "OPEN" id page)
         (swap! *pages update page assoc id {:ch ch, :page page, :platform platform, :joined (Instant/now)}))
       :on-close
       (fn [ch status]
         ; (prn "CLOSE" id page status)
         (swap! *pages update page dissoc id))
       :on-receive
       (fn [ch msg]
         ; (prn "RCV" id page msg)
         (when-some [[_ x y] (re-find #"\[\s*([0-9]+),\s*([0-9]+)\s*\]" msg)]
           (swap! *pages update page update id assoc
             :x       (parse-long x)
             :y       (parse-long y)
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
              td, th { text-align: left; min-width: 100px; padding: 0 5px; }"]
        [:table
         [:thead
          [:tr
           [:th "id"]
           [:th "x"]
           [:th "y"]
           [:th "platform"]
           [:th "joined"]
           [:th "updated"]]]
         [:tbody
          (for [[page clients] @*pages]
            (list
              [:tr
               [:th {:colspan 6} page]]
              (for [[id client] clients]
                [:tr
                 [:th (str id)]
                 [:td (str (:x client))]
                 [:td (str (:y client))]
                 [:td (:platform client)]
                 [:td (core/format-temporal (:joined client) "yyyy-MM-dd hh:mm:ss")]
                 [:td (core/format-temporal (:updated client) "yyyy-MM-dd hh:mm:ss")]])))]])}
     default/default
     :content
     render/render-html)})

(def routes
  {"GET /ptrs" handler
   "GET /ptrs/stats" stats})

(defn page-msg [page]
  (let [clients    (get @*pages page)
        client-pos (fn [[id client]]
                     (let [{:keys [x y platform]} client]
                       (when (and id x y platform)
                         (str "[" id "," (:x client) "," (:y client) ",\"" platform "\"]"))))]
    (str "[" (str/join "," (keep client-pos clients)) "]")))

(defn broadcast []
  (let [[dirty _] (reset-vals! *dirty #{})]
    (doseq [page dirty
            :let [msg (page-msg page)]
            [id client] (get @*pages page)
            :let [ch (:ch client)]
            :when ch]
      (http/send! ch msg))))

(mount/defstate ^TimerTask broadcast-task
  :start
  (schedule broadcast 0 1000)
  :stop
  (.cancel ^TimerTask broadcast-task))
