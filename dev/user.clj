(ns user
  (:require
    [clj-reload.core :as reload]
    [clojure.core.server :as server]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :as test]
    [duti.core :as duti]
    [mount.core :as mount]))

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
    (duti/start-socket-repl {:port (some-> port parse-long)})))

(defn test-all []
  (reload {:only #"site\..*-test"})
  (duti/test-throw #"site\..*-test"))

(defn -test-main [_]
  (reload {:only #"site\..*-test"})
  (duti/test-exit #"site\..*-test"))

(comment
  (reload)
  (test-all))
