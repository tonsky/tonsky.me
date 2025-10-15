(ns user
  (:require
   [clj-reload.core :as reload]
   [clojure+.core.server :as server]
   [clojure+.error]
   [clojure+.hashp]
   [clojure+.print]
   [clojure+.test]
   [mount.core :as mount]))

(clojure+.error/install!
  {:trace-transform
   (fn [trace]
     (take-while #(not (#{"Compiler" "clj-reload" "clojure-sublimed"} (:ns %))) trace))})

(clojure+.hashp/install!)
(clojure+.print/install!)
(clojure+.test/install!)

(reload/init
  {:dirs ["src" "dev" "test"]
   :no-reload #{'user}})

(defn reload [& [opts]]
  (set! *warn-on-reflection* true)
  (let [res (reload/reload opts)
        cnt (count (:loaded res))]
    ((requiring-resolve 'site.core/apply-args) *command-line-args*)
    (mount/start)
    (str "Ready – " cnt " " (if (= 1 cnt) "ns" "nses"))))

(defn -main [& args]
  (alter-var-root #'*command-line-args* (constantly args))
  ((requiring-resolve 'site.core/apply-args) *command-line-args*)
  (require 'site.server)
  (mount/start)
  (let [{port "--repl-port"} args]
    (server/start-server {:port (some-> port parse-long)})))

(defn test-all []
  (reload {:only #"site\..*-test"})
  (clojure+.test/run #"site\..*-test"))

(defn -test-main [_]
  (let [{:keys [fail error]} (test-all)]
    (System/exit (+ fail error))))
