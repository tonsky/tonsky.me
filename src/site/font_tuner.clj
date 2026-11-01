(ns site.font-tuner
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [mount.core :as mount]
   [site.core :as core]))

;; Google Fonts

(def metadata-url
  "https://fonts.google.com/metadata/fonts")

(defn- style-weight [style]
  (parse-long (re-find #"\d+" style)))

(defn- axis-range [axes axis]
  (some (fn [{:keys [tag min max]}]
          (when (= axis tag)
            (mapv #(if (== % (Math/floor %)) (long %) %) [min max])))
    axes))

(defn- family [{:keys [family category fonts axes]}]
  (let [styles (map name (keys fonts))]
    (core/some-map
      :family   family
      :category category
      :weights  (->> styles (remove #(str/ends-with? % "i")) (map style-weight) sort vec)
      :italics  (->> styles (filter #(str/ends-with? % "i")) (map style-weight) sort vec)
      :wght     (axis-range axes "wght")
      :wdth     (axis-range axes "wdth")
      :opsz     (axis-range axes "opsz"))))

(def google-fonts
  (core/memoize-by (constantly true)
    (fn []
      (->> (json/parse-string (slurp metadata-url) true)
        :familyMetadataList
        (mapv family)))))

;; Local fonts

(def fonts-dir
  (io/file (System/getProperty "user.home") "Library/Fonts"))

(def formats
  "Extensions browsers load via @font-face, most preferred first: [ext format mime-type]"
  [["woff2" "woff2"    "font/woff2"]
   ["woff"  "woff"     "font/woff"]
   ["ttf"   "truetype" "font/ttf"]
   ["otf"   "opentype" "font/otf"]])

(def fc-weights
  "fontconfig weight -> CSS weight, see FcWeightToOpenType"
  [[0 100] [40 200] [50 300] [55 350] [75 380] [80 400] [100 500] [180 600] [200 700] [205 800] [210 900] [215 1000]])

(defn- css-weight [fc]
  (or
    (some (fn [[[f0 c0] [f1 c1]]]
            (when (<= f0 fc f1)
              (Math/round (double (+ c0 (* (- c1 c0) (/ (- fc f0) (- f1 f0))))))))
      (partition 2 1 fc-weights))
    (if (neg? fc) 100 1000)))

(defn- parse-range
  "'80' -> [80.0 80.0], '[50 210]' -> [50.0 210.0]"
  [s]
  (let [[lo hi] (map parse-double (re-seq #"[\d.]+" s))]
    (when lo
      [lo (or hi lo)])))

(defn- normalize-family
  "Variable fonts often have their own family: 'Inter Variable', 'X VF Trial', 'Y VAR'"
  [family]
  (-> family
    (str/replace #"\s+(Variable|VF|VAR)\b" "")
    str/trim))

(def junk-family
  "Family names that come from broken name tables"
  #"(?i)^\W*$|copyright|^FwTRIAL\W*$")

(def file-style-words
  #{"thin" "hairline" "extra" "ultra" "semi" "demi" "light" "book" "regular" "normal" "roman" "medium" "bold" "black" "heavy"
    "xlight" "xbold" "xblack" "italic" "ita" "retalic" "oblique" "web" "webfont"
    "cond" "condensed" "narrow" "compressed" "wide" "extended" "expanded" "var" "vf" "variable"})

(def file-weights
  [[#"extralight|ultralight|xlight" 200] [#"semibold|demibold" 600] [#"extrabold|ultrabold|xbold" 800]
   [#"thin|hairline" 100] [#"light" 300] [#"medium" 500] [#"bold" 700] [#"black|heavy" 900]])

(defn- parse-file-name
  "'FwTRIAL-Nikolai-CondLightItalicWeb.woff2' -> {:family 'FwTRIAL Nikolai', :weight 300, :italic true, :width 1}"
  [file]
  (let [base   (-> (io/file file) .getName (str/replace #"\..*$" ""))
        tokens (->> (str/split base #"[-_\s]+")
                 (mapcat #(str/split % #"(?<=[a-z])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=(VAR|VF)([A-Z]|\b))|(?<=VAR|VF)(?=[A-Z])"))
                 (remove str/blank?))
        [family style] (split-with #(not (file-style-words (str/lower-case %))) tokens)
        style  (str/lower-case (str/join style))]
    {:family (->> family
               (map #(if (re-matches #"[a-z].*" %) (str/capitalize %) %))
               (str/join " "))
     :weight (or (some (fn [[re w]] (when (re-find re style) w)) file-weights) 400)
     :italic (boolean (re-find #"ita|retalic|oblique" style))
     :width  (if (re-find #"cond|narrow|compressed|wide|extended|expanded" style) 1 0)}))

(defn- parse-face [line]
  (let [[family weight slant width variable size file] (str/split line #"\t" 7)
        ext (some-> file (->> (re-find #"\.([^./]+)$")) second str/lower-case)
        [w0 w1] (some-> weight parse-range)
        [s0]    (some-> slant parse-range)
        [d0 d1] (some-> width parse-range)
        [o0 o1] (some-> size parse-range)] ; fontconfig maps opsz axis to size
    (when (and
            family
            (some #(= ext (first %)) formats)
            w0 s0 d0)
      (let [variable? (= "True" variable)
            face      {:family   family
                       :opsz     (when (and variable? o0 (< o0 o1)) [o0 o1])
                       :wdth     (when (and variable? (< d0 d1)) [d0 d1]) ; fontconfig maps wdth axis to width
                       :italic   (pos? s0)
                       :variable variable?
                       :weight   [(css-weight w0) (css-weight w1)]
                       :width    (cond (<= d0 100 d1) 0 (< d1 100) (- 100 d1) :else (- d0 100)) ; distance from normal width
                       :ext      ext
                       :file     file}
            face      (if (re-find junk-family family)
                        ;; name table is garbage, and so are style/weight: take them from file name instead
                        (let [parsed (parse-file-name file)]
                          (cond-> (merge face (select-keys parsed [:family :width]))
                            (:italic parsed) (assoc :italic true)
                            (not variable?)  (assoc :weight [(:weight parsed) (:weight parsed)])))
                        face)]
        (when (re-find #"\p{L}" (:family face))
          (update face :family normalize-family))))))

(defn- woff2-faces
  "fc-scan can't read woff2 without FreeType built with Brotli, so everything here comes from file name"
  []
  (for [file  (file-seq fonts-dir)
        :when (re-find #"(?i)\.woff2$" (.getName ^java.io.File file))
        :let  [{:keys [family weight italic width]} (parse-file-name file)
               variable? (boolean (re-find #"VF|VAR|(?i:variable)" (.getName ^java.io.File file)))]
        :when (re-find #"\p{L}" family)]
    {:family   family
     :italic   italic
     :variable variable?
     :weight   (if variable? [100 900] [weight weight]) ; browser clamps to actual wght axis
     :width    width
     :ext      "woff2"
     :file     (str file)}))

(defn- format-rank [face]
  (count (take-while #(not= (:ext face) (first %)) formats)))

(defn- pick-face
  "One face per family: variable if there is one, otherwise the one closest to Regular 400.
   Upright over italic, normal width over others, then by format preference"
  [faces]
  (first
    (sort-by
      (fn [{:keys [italic variable width weight] :as face}]
        [(if italic 1 0)
         (if variable 0 1)
         width
         (Math/abs (long (- 400 (first weight))))
         (format-rank face)])
      faces)))

(defn- weight-variant?
  "'Nikolai Semibold' when 'Nikolai' exists too"
  [families family]
  (let [base (str/replace family #"(?i)\s+((Extra|Ultra|Semi|Demi)\s*)?(Thin|Hairline|Light|Book|Medium|Bold|Black|Heavy)$" "")]
    (and (not= base family) (contains? families base))))

(defn scan []
  (let [{:keys [out]} (shell/sh "fc-scan" "--format" "%{family[0]}\t%{weight}\t%{slant}\t%{width}\t%{variable}\t%{size}\t%{file}\n" (str fonts-dir))
        faces (->> (str/split-lines out)
                (keep parse-face))
        faces (let [scanned (set (map :family faces))] ; scanned faces know their real axes
                (concat faces (remove #(scanned (:family %)) (woff2-faces))))
        names (set (map :family faces))
        faces (->> faces
                (remove #(weight-variant? names (:family %)))
                (group-by :family)
                vals
                (map pick-face)
                (sort-by #(str/lower-case (:family %)))
                (map-indexed #(assoc %2 :id %1))
                vec)]
    {:faces    faces
     :families (mapv #(into {} (filter val) (select-keys % [:family :variable :wdth :opsz])) faces)}))

(mount/defstate local-fonts
  :start (when (and core/dev? core/font-tuner?)
           (future
             (let [res (scan)]
               (println "Font tuner: found" (count (:families res)) "local font families")
               res))))

(defn- local-css [family]
  (str/join "\n"
    (for [{:keys [id italic weight ext] face-family :family} (:faces @local-fonts)
          :when (= family face-family)
          :let [[lo hi] weight]]
      (str "@font-face { font-family: '" family " (local)'; "
        "font-style: " (if italic "italic" "normal") "; "
        "font-weight: " (if (= lo hi) lo (str lo " " hi)) "; "
        "src: url('/font-tuner/file/" id "') format('" (some (fn [[e f _]] (when (= e ext) f)) formats) "'); }"))))

;; Handlers

(defn- enabled? []
  (and core/dev? core/font-tuner?))

(defn fonts [_req]
  (when (enabled?)
    {:status  200
     :headers {"Content-Type" "application/json; charset=UTF-8"}
     :body    (json/generate-string
                {:google (try
                           (google-fonts)
                           (catch Exception e
                             (println "Font tuner: failed to fetch Google Fonts:" (ex-message e))
                             []))
                 :local  (:families @local-fonts)})}))

(defn css [req]
  (when (enabled?)
    {:status  200
     :headers {"Content-Type" "text/css; charset=UTF-8"}
     :body    (local-css (get-in req [:query-params "family"]))}))

(defn file [req]
  (when (enabled?)
    (let [[id] (:path-params req)]
      (when-some [{:keys [file ext]} (get (:faces @local-fonts) (parse-long id))]
        {:status  200
         :headers {"Content-Type" (some (fn [[e _ mime]] (when (= e ext) mime)) formats)}
         :body    (io/file file)}))))
