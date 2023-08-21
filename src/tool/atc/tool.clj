(ns atc.tool
  (:require
   [atc.data.core :refer [coord-distance]]
   [atc.kmz :as kmz]
   [atc.nasr :as nasr]
   [atc.nasr.airac :refer [airac-data]]
   [atc.pronunciation :as pronunciation]
   [atc.util.with-timing :refer [with-timing]]
   [babashka.cli :as cli]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.set :refer [map-invert]]
   [clojure.string :as str]
   [clojure.walk :as w]))

(defn- compose-runways [data]
  (mapv
    (fn [rwy]
      {:start-id (:base-end-id rwy)
       :start-threshold [(:base-end-latitude rwy) (:base-end-longitude rwy)]
       :end-id (:reciprocal-end-id rwy)
       :end-threshold [(:reciprocal-end-latitude rwy) (:reciprocal-end-longitude rwy)]})
    (:rwy data)))

(defn- format-navaids
  ([items]
   (map
     (fn [fix]
       (let [base {:id (:id fix)
                   :position [(:latitude fix) (:longitude fix)]
                   :type (:type fix)}
             raw-pronunciation (or (:radio-voice-name fix)
                                   (:name fix)
                                   (:id fix))

             cleaned-pronunciation (when raw-pronunciation
                                     (-> raw-pronunciation
                                         (str/lower-case)
                                         (str/replace #"[-]+" " ")))
             pronunciation (when cleaned-pronunciation
                             (if (pronunciation/pronounceable? cleaned-pronunciation)
                               cleaned-pronunciation
                               (pronunciation/make-pronounceable
                                 cleaned-pronunciation)))]
         (cond-> base
           (and (some? cleaned-pronunciation)
                (not= raw-pronunciation (:id fix)))
           (assoc :name cleaned-pronunciation)

           (and (some? pronunciation)
                (not= pronunciation cleaned-pronunciation))
           (assoc :pronunciation pronunciation))))
     items))
  ([type items]
   (format-navaids
     (map #(assoc % :type type) items))))

(defn- format-arrival-route [{:keys [origin route-string]}]
  [origin {:route route-string}])

(defn- format-departure [departure]
  (let [id (:computer-code departure)
        part-fn (if (and (not= :arrival (:type departure))
                         (:transitions departure))
                  first
                  last)
        id (part-fn (str/split id #"\."))
        path (->> departure
                  :paths
                  first
                  (remove #(= :aa (:type %)))
                  (mapv (comp #(hash-map :fix %) :fix-id)))]
    [id {:path path}]))

(defn- format-departure-route [route]
  [(:destination route) {:fix (:departure-fix route)
                         :route (:route-string route)}])

(defn- format-center-facility [facility]
  {:id (:site-location facility)
   :position [(:latitude facility) (:longitude facility)]
   :frequency (:frequency facility)})

(defn- resolve-departure-fix-collisions [collisions codings]
  (loop [collisions collisions
         codings codings]
    (letfn [(choose-code [word]
              (loop [letters (map str word)]
                (if-let [ch (first letters)]
                  (if (contains? codings ch)
                    (recur (next letters))
                    ch)

                  ; No more options. Pick the first available
                  (->> (range (int \Z) (int \A) -1)
                       (map (comp str char))
                       (remove (partial contains? codings))
                       first))))]
      (if-let [word (first collisions)]
        (recur
          (next collisions)
          (assoc codings (choose-code word) word))
        codings))))

(defn- generate-departure-fix-codings [fix-names]
  (loop [fix-names (->> (into #{} fix-names)
                        (sort-by (juxt #(- 10 (count %))
                                       identity)))
         collisions []
         codings {}]
    (if-let [next-name (first fix-names)]
      (let [code (str (first next-name))
            clean? (not (contains? codings code))]
        (if clean?
          (recur (next fix-names)
                 collisions
                 (assoc codings code next-name))
          (recur (next fix-names)
                 (conj collisions next-name)
                 codings)))

      (-> (resolve-departure-fix-collisions collisions codings)
          (map-invert)))))

(defn build-airport [zip-file kmz-file icao]
  (let [all-facilities (future (with-timing "all-facilities"
                                 (nasr/find-facilities zip-file)))
        {[apt] :apt :as data} (with-timing "apt"
                                (nasr/find-airport-data zip-file icao))
        runways (compose-runways data)
        position [(:latitude apt) (:longitude apt) (:elevation apt)]
        all-frequencies (future (nasr/find-terminal-frequencies zip-file (:id apt)))

        procedures (with-timing "procedures"
                     (nasr/find-procedures zip-file (:id apt)))
        departures (with-timing "departures"
                     (nasr/find-departure-routes zip-file icao))
        arrivals (future
                   (with-timing "arrivals"
                     (nasr/find-arrival-routes zip-file icao)))

        ; TODO read vor/dmes from here
        departure-exit-fix-names (->> departures
                                      (map :departure-fix))

        procedure-navaids (->> procedures
                               (mapcat (juxt :paths
                                             (comp #(mapcat :fixes %) :transitions)))
                               flatten)
        procedure-fix-ids (->> procedure-navaids
                               (filter #(= :r (:type %)))
                               (map :fix-id))
        procedure-navaid-ids (->> procedure-navaids
                                  (filter #(not= :r (:type %)))
                                  (map :fix-id))

        fixes (future
                (with-timing "fixes"
                  (nasr/find-fixes
                    zip-file
                    :ids (concat procedure-fix-ids departure-exit-fix-names))))
        navaids (future
                  (with-timing "navaids"
                    (nasr/find-navaids
                      zip-file
                      :ids (concat procedure-navaid-ids departure-exit-fix-names))))

        closest-centers (future
                          (with-timing "closest-centers"
                            (->> @all-facilities
                                 (filter :latitude)
                                 (sort-by
                                   #(do
                                      (coord-distance
                                        position
                                        [(:latitude %)
                                         (:longitude %)]))))))

        airspace (future
                   (with-timing "airspace"
                     (->> @all-facilities
                          (filter #(= (:boundary-artcc-id apt)
                                      (:artcc %)))
                          first
                          :name
                          (kmz/find-airspace-regions-in-kmz kmz-file))))]

    {:id (:icao apt)
     :name (:name apt)
     :magnetic-north (let [raw-variation (:magnetic-variation apt)
                           declination (case (last raw-variation)
                                         \W -1
                                         \E 1)]
                       (* declination
                          (Double/parseDouble
                            (subs raw-variation 0 (dec (count raw-variation))))))
     :position position
     :runways runways
     :navaids (->> (format-navaids :fix @fixes)
                   (concat (format-navaids @navaids))

                   ; TODO Also, include VORs, etc.
                   ; TODO Also also, include fixes on airways, stars/sids, etc.
                   (sort-by :id)
                   vec)

     :arrivals (->> procedures
                    (into
                      {}
                      (comp
                        (filter #(= :arrival (:type %)))
                        (map format-departure))))

     :arrival-routes (->> @arrivals
                          (group-by :origin)
                          (into {}
                                (comp
                                  (map val)
                                  (map first)
                                  (map format-arrival-route))))

     :departures (->> procedures
                      (into
                        {}
                        (comp
                          (filter #(= :departure (:type %)))
                          (map format-departure))))

     :departure-routes (->> departures
                            (into
                              {}
                              (map format-departure-route)))

     :departure-fix-codes (->> departures
                               (map :departure-fix)
                               (generate-departure-fix-codings))

     :center-facilities (->> @closest-centers
                             (take 4)
                             (mapv format-center-facility))

     :positions {:cd {:frequency (->> @all-frequencies
                                      (filter #(and
                                                 (nil? (:sectorization %))
                                                 (str/starts-with? (:usage %) "CD")))
                                      first
                                      :frequency)
                      :track-symbol "D"}
                 :twr {:frequency (->> @all-frequencies
                                       (filter #(= "LCL/P" (:usage %)))
                                       first
                                       :frequency)
                       :track-symbol "T"}
                 :gnd {:frequency (->> @all-frequencies
                                       (filter #(= "GND/P" (:usage %)))
                                       first
                                       :frequency)
                       :track-symbol "G"}
                 ; NOTE: app/dep positions aren't always provided...
}

     :airspace-geometry (vec @airspace)}))

(defn- pretty [v]
  (with-out-str
    (->> v
         (w/prewalk
           (fn [form]
             (if (and (symbol? form)
                      (#{"atc.tool" "clojure.core"}
                        (namespace form)))
               (symbol (name form))
               form)))
         pprint)))

(defn- build-airport-cli [{{:keys [icao nasr-path write]} :opts}]
  {:pre [icao nasr-path]}
  (let [icao (str/upper-case icao)

        destination-dir (io/file nasr-path)
        airac (airac-data)
        zip-file (nasr/locate-zip airac destination-dir)
        kmz-file (kmz/locate-kmz airac destination-dir)

        airport (with-timing "build-airport"
                  (build-airport zip-file kmz-file icao))]
    (pprint airport)

    (when-let [unpronounceable (with-timing "unpronounceable"
                                 (->> (:navaids airport)
                                      (map #(or (:pronunciation %)
                                                (:name %)
                                                (:id %)))
                                      (pronunciation/unpronounceable)
                                      seq))]
      (println "WARNING: Detected" (count unpronounceable) "unpronounceable navaids:")
      (println "\t" unpronounceable))

    (when write
      (let [icao-sym (str/lower-case icao)
            file-path (str "src/main/atc/data/airports/" icao-sym ".cljc")
            airport-ns (symbol
                         (str "atc.data.airports." icao-sym))]
        (println "Writing to: " file-path)
        (->>
          `(do
             (ns ~airport-ns
               (:require
                 [atc.voice.parsing.airport :as parsing]
                 [atc.util.instaparse :refer-macros [defalternates-expr]]))

             (def airport ~airport)

             (def navaids-by-pronunciation
               (parsing/airport->navaids-by-pronunciation airport))

             (defalternates-expr navaid-pronounced
               (keys navaids-by-pronunciation))

             (def exports
               {:airport airport
                :navaids-by-pronunciation navaids-by-pronunciation
                :navaid-pronounced navaid-pronounced}))

          (drop 1)
          (map pretty)
          (str/join "\n")
          (spit (io/file file-path)))
        (println "... done!")))))

(defn- pronounceable-cli [{{:keys [word]} :opts}]
  (if-let [missing (pronunciation/missing-words word)]
    (println "Unable to pronunce `" word "`; missing: " missing)
    (println "`" word "` is pronounceable!")))

(def ^:private cli-table
  [{:cmds ["build-airport"] :fn build-airport-cli
    :exec-args {:nasr-path "."}
    :args->opts [:icao]}
   {:cmds ["pronounceable"] :fn pronounceable-cli
    :args->opts [:word]}])

(defn -main [& args]
  (cli/dispatch cli-table args))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(comment
  (def zip-file (let [destination-dir (io/file ".")
                      airac (airac-data)]
                  (nasr/locate-zip airac destination-dir)))

  (-main "build-airport" "kjfk" #_"--write")
  (-main "build-airport" "kjfk" "--write"))
