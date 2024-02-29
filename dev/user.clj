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

(defn reload []
  (set! *warn-on-reflection* true)
  (let [res (reload/reload)
        cnt (count (:loaded res))]
    ((requiring-resolve 'site.core/apply-args) *command-line-args*)
    (mount/start)
    (str "Ready – " cnt " " (if (= 1 cnt) "ns" "nses"))))

(defn -main [& args]
  (alter-var-root #'*command-line-args* (constantly args))
  (require 'site.server)
  ((requiring-resolve 'site.core/apply-args) *command-line-args*)
  (mount/start)
  (let [{port "--repl-port"} args]
    (duti/start-socket-repl {:port (some-> port parse-long)})))

(defn test-all []
  (require 'site.parser-test)
  (reload)
  (duti/test-throw #"site\..*-test"))

(defn -test-main [_]
  (require 'clj-reload.parser-test)
  (duti/test-exit #"site\..*-test"))

(comment
  (reload)
  (test-all))
