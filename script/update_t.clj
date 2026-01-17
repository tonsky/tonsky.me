#!/usr/bin/env bb

(require
  '[clojure.string :as str]
  '[babashka.fs :as fs])

(def css-path
  "site/style.css")

(defn file-mtime [path]
  (-> path fs/file .lastModified (quot 1000)))

(defn update-url [match]
  (let [full-match (first match)
        path       (second match)
        file-path  (str "site" path)
        ;; Remove existing ?t=... if present
        clean-path (str/replace path #"\?t=\d+" "")]
    (if (fs/exists? file-path)
      (let [mtime (file-mtime file-path)]
        (str "url(" clean-path "?t=" mtime ")"))
      (do
        (println "Warning: file not found:" file-path)
        full-match))))

(defn update-css [content]
  (str/replace content
    #"url\(\"?(/i/[^\")\s]+)\"?\)"
    update-url))

(defn -main []
  (let [content     (slurp css-path)
        new-content (update-css content)]
    (spit css-path new-content)
    (println "Updated" css-path)))

(-main)
