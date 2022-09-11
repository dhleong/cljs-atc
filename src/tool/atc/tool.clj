(ns atc.tool
  (:require
   [atc.nasr :as nasr]
   [atc.nasr.airac :refer [airac-data]]
   [atc.pronunciation :as pronunciation]
   [babashka.cli :as cli]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]))

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
                                   (:name fix))

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
                (not= raw-pronunciation (:id fix))
                (not= pronunciation cleaned-pronunciation))
           (assoc :pronunciation pronunciation))))
     items))
  ([type items]
   (format-navaids
     (map #(assoc % :type type) items))))

(defn build-airport [zip-file icao]
  (let [{[apt] :apt :as data} (time (nasr/find-airport-data zip-file icao))
        runways (compose-runways data)
        position [(:latitude apt) (:longitude apt) (:elevation apt)]

        procedures (time (nasr/find-procedures zip-file (:id apt)))
        departures (time (nasr/find-departure-routes zip-file icao))

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
                (time (nasr/find-fixes
                        zip-file
                        :ids (concat procedure-fix-ids departure-exit-fix-names))))
        navaids (future
                  (time (nasr/find-navaids
                          zip-file
                          :ids (concat procedure-navaid-ids departure-exit-fix-names))))]

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
                   vec)}))

(defn- build-airport-cli [{{:keys [icao nasr-path write]} :opts}]
  {:pre [icao nasr-path]}
  (let [icao (str/upper-case icao)

        destination-dir (io/file nasr-path)
        airac (airac-data)
        zip-file (nasr/locate-zip airac destination-dir)

        airport (build-airport zip-file icao)]
    (pprint airport)

    (when-let [unpronounceable (time
                                 (doall
                                   (pronunciation/unpronounceable
                                     (map #(or (:pronunciation %)
                                               (:id %))
                                          (:navaids airport)))))]
      (println "WARNING: Detected" (count unpronounceable) "unpronounceable navaids:")
      (println "\t" unpronounceable))

    (when write
      (let [icao-sym (str/lower-case icao)
            file-path (str "src/main/atc/data/airports/" icao-sym ".cljc")]
        (println "Writing to: " file-path)
        (spit
          (io/file file-path)
          (str
            (format "(ns atc.data.airports.%s)\n\n" icao-sym)
            (with-out-str
              (pprint (cons (symbol "def airport")
                            (list airport))))))
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

(comment
  (-main "build-airport" "kjfk"))
