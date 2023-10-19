(ns site.pages.patrons
  (:require
    [cheshire.core :as json]
    [toml-clj.core :as toml]
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [mount.core :as mount]
    [org.httpkit.client :as http]
    [site.cache :as cache]
    [site.core :as core])
  (:import
    [java.io File]
    [java.time LocalDate ZonedDateTime]))

(defn print-progress [prefix percent]
  (let [percent-int (int (* percent 20))]
    (print prefix "[ ")
    (dotimes [_ percent-int]
      (print "▓"))
    (dotimes [_ (- 20 percent-int)]
      (print "░"))
    (println " ]")))

;; PATRONS

(defn fetch-patrons []
  (let [url     "https://www.patreon.com/api/oauth2/v2/campaigns/2077079/members"
        token   (System/getenv "PATREON_ACCESS_TOKEN")
        _       (when (str/blank? token)
                  (throw (ex-info "PATREON_ACCESS_TOKEN is not set" {})))
        headers {"Authorization" (str "Bearer " token)}
        query   {"fields[member]" (str/join ","
                                    ["currently_entitled_amount_cents"
                                     "pledge_cadence"
                                     "full_name"
                                     "last_charge_status"
                                     "patron_status"
                                     "email"
                                     ; "last_charge_date"
                                     "lifetime_support_cents"
                                     ; "pledge_relationship_start"
                                     ; "will_pay_amount_cents"
                                     ])}]
    (print-progress "Fetching patrons" 0)
    (loop [cursor  nil
           patrons []]
      (let [query'   (cond-> query
                       (some? cursor) (assoc "page[cursor]" cursor))
            response (-> (http/get url {:query-params query' :headers headers :as :text})
                       (deref)
                       :body
                       (json/parse-string true))
            patrons' (into patrons (:data response))]
        (print-progress "Fetching patrons" (/ (count patrons') (-> response :meta :pagination :total)))
        (if-some [cursor' (-> response :meta :pagination :cursors :next)]
          (recur cursor' patrons')
          (do
            (println)
            patrons'))))))

(defn patron-active? [{:keys [attributes] :as patron}]
  (and (= "Paid" (:last_charge_status attributes))
    (= "active_patron" (:patron_status attributes))))

(defn normalize-patron [{:keys [attributes] :as patron}]
  {:name      (str/trim (:full_name attributes))
   :id        (:email attributes)
   :pledge    (-> (:currently_entitled_amount_cents attributes)
                (/ (:pledge_cadence attributes))
                (/ 100)
                (int))
   :platforms #{:patreon}})

(defn patrons []
  (->> (fetch-patrons)
    (filter patron-active?)
    (mapv normalize-patron)))

;; SPONSORS

(defn sponsors-query [cursor]
  (->
    (str 
      "{\"query\": \"{
  viewer {
    sponsorshipsAsMaintainer(first: 10, after: " cursor ") {
      totalCount
      pageInfo {
        hasNextPage, endCursor
      }
      nodes {
        createdAt, id
        sponsorEntity {
          ... on User {
            id, user_email: email, name, login
          }
          ... on Organization {
            id, org_email: email, name, login
          }
        }
        tier {
          monthlyPriceInDollars
        }
      }
    }
  }
}\"}")
    (clojure.string/replace "\n" "\\n")))

(defn fetch-sponsors []
  (let [url     "https://api.github.com/graphql"
        token   (System/getenv "GITHUB_TOKEN_SPONSORS")
        _       (when (str/blank? token)
                  (throw (ex-info "GITHUB_TOKEN_SPONSORS is not set" {})))
        headers {"Authorization" (str "Bearer " token)}]
    (print-progress "Fetching sponsors" 0)
    (loop [cursor   nil
           sponsors []]
      (let [query   (if (some? cursor)
                      (sponsors-query (str "\\\"" cursor "\\\""))
                      (sponsors-query "null"))
            response (-> (http/post url {:body query :headers headers :as :text})
                       (deref)
                       :body
                       (json/parse-string true))
            sponsors' (into sponsors
                        (-> response :data :viewer :sponsorshipsAsMaintainer :nodes))]
        (print-progress "Fetching sponsors"
          (/ (count sponsors')
            (-> response :data :viewer :sponsorshipsAsMaintainer :totalCount)))
        (if (-> response :data :viewer :sponsorshipsAsMaintainer :pageInfo :hasNextPage)
          (recur (-> response :data :viewer :sponsorshipsAsMaintainer :pageInfo :endCursor) sponsors')
          (do
            (println)
            sponsors'))))))

(defn normalize-sponsor [{entity :sponsorEntity :as sponsor}]
  (let [email (or (:user_email entity) (:org_email entity))]
    {:name      (str/trim (or (:name entity) (:login entity)))
     :id        (if (str/blank? email)
                  (:login entity)
                  email)
     :pledge    (-> sponsor :tier :monthlyPriceInDollars)
     :platforms #{:github}}))

(defn sponsors []
  (->> (fetch-sponsors)
    (filter #(not-empty (:sponsorEntity %)))
    (mapv normalize-sponsor)))


;; FETCHING

(declare fetch-members)

(defn schedule-soon [_]
  (let [when (-> (ZonedDateTime/now core/UTC)
               (.plusHours 1))]
    (println "Scheduling patrons fetching at" (core/format-temporal when "yyyy-MM-dd HH:mm:ssX"))
    (core/schedule-once fetch-members when)))

(defn schedule-in-a-month [_]
  (let [when (-> (LocalDate/now core/UTC)
               (.withDayOfMonth 6)
               (.plusMonths 1)
               (.atStartOfDay core/UTC))]
    (println "Scheduling patrons fetching at" (core/format-temporal when "yyyy-MM-dd HH:mm:ssX"))
    (core/schedule-once fetch-members when)))

(mount/defstate fetch-members-task
  :start (schedule-in-a-month nil)
  :stop  (do
           (println "Stopping patrons fetching")
           (core/cancel-task fetch-members-task)))

(defn merge-members [m1 m2]
  (-> m1
    (update :pledge + (:pledge m2))
    (update :platforms clojure.set/union (:platforms m2))))

(defn fetch-members []
  (try
    (let [now  (core/now)
          file (io/file (str "site/patrons/" (core/format-temporal now "yyyy-MM") "-patrons.toml"))]
      (when-not (.exists file)
        (let [patrons  (patrons)
              sponsors (sponsors) 
              members  (->>
                         (merge-with merge-members
                           (core/map-by :id patrons)
                           (core/map-by :id sponsors))
                         (vals)
                         (sort-by (juxt #(- (:pledge %)) :name) core/reverse-compare))]
          (with-open [writer (io/writer file)]
            (binding [*out* writer]
              (println "fetched =" (core/format-temporal now "yyyy-MM-dd'T'HH:mm:ssX\n"))
              (doseq [{:keys [name id pledge platforms]} members]
                (println "[[member]]")
                (println (str "id        = \"" id "\""))
                (println (str "name      = \"" name "\""))
                (println (str "pledge    = " pledge))
                (println (str "platforms = [\"" (str/join "\", \"" (map clojure.core/name platforms)) "\"]\n")))))))
      (alter-var-root #'fetch-members-task schedule-in-a-month))
    (catch Exception e
      (alter-var-root #'fetch-members-task schedule-soon)
      (throw e))))

;; PAGE

(defn last-file ^File []
  (->> (file-seq (io/file "site/patrons"))
    (next)
    (filter #(str/ends-with? (.getName ^File %) ".toml"))
    (sort-by #(.getName ^File %) core/reverse-compare)
    (first)))

(defn set-layer [member]
  (assoc member :layer
    (condp <= (:pledge member)
      50 :sponsor
      10 :supporter
      5  :fan
      nil)))

(def members
  (core/memoize-by
    #(.lastModified (cache/touched (last-file)))
    (fn []
      (let [parsed (toml/read-string (slurp (last-file)) {:key-fn keyword})]
        {:fetched (:fetched parsed)
         :members (->> (:member parsed)
                    (map set-layer)
                    (vec))}))))

(defn page []
  (let [members (members)
        layers  (group-by :layer (:members members))]
    {:title  "Patrons"
     :uri    "/patrons/"
     :styles ["/patrons/patrons.css"]
     :content
     [:.content
      [:h1.title "People supporting my work"]
      [:h2 "on " [:a {:href "https://patreon.com/tonsky"} "Patreon"] " and " [:a {:href "https://github.com/sponsors/tonsky"} "Github Sponsors"]]
      [:.foot "Last updated " (core/format-temporal (:fetched members) "MMMM d, yyyy")]
      (for [layer [:sponsor :supporter :fan]
            :let [members (get layers layer)]
            :when (seq members)]
        (list
          (let [name (str/capitalize (name layer))]
            [:h2 (core/pluralize (count members) name (str name "s"))])
          [:ul
           (for [member (sort-by (juxt #(- (:pledge %)) :name) members)]
             [:li (:name member)])]))
      [:h2 "— Thank you!"]]}))
