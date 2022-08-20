(ns atc.tool
  (:require
   [atc.nasr :as nasr]
   [atc.nasr.airac :refer [airac-data]]
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

(defn- format-fixes [fixes]
  (map
    (fn [fix]
      {:id (:id fix)
       :position [(:latitude fix) (:longitude fix)]
       :type :fix})
    fixes))

(defn build-airport [zip-file icao]
  (let [{[apt] :apt :as data} (time (nasr/find-airport-data zip-file icao))
        runways (compose-runways data)
        position [(:latitude apt) (:longitude apt) (:elevation apt)]

        procedures (time (nasr/find-procedures zip-file (:id apt)))
        departures (time (nasr/find-departure-routes zip-file icao))

        ; TODO read vor/dmes from here
        departure-exit-navaids (->> departures
                                    (map :departure-fix))

        procedure-navaids (->> procedures
                               (mapcat (juxt :paths
                                             (comp #(mapcat :fixes %) :transitions)))
                               flatten)
        procedure-fix-ids (->> procedure-navaids
                               (filter #(= :r (:type %)))
                               (map :fix-id))

        fixes (time (nasr/find-fixes
                      zip-file
                      :ids (concat procedure-fix-ids departure-exit-navaids)
                      ; :near position
                      ; :in-range (* 100 1000)
                      ; :on-charts #{:sid :star}
))]

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
     :navaids (->> (format-fixes fixes)
                   ; TODO Also, include VORs, etc.
                   ; TODO Also also, include fixes on airways, stars/sids, etc.
                   (sort-by :id)
                   vec)}))

(defn- build-airport-cli [{{:keys [icao nasr-path]} :opts}]
  {:pre [icao nasr-path]}
  (let [icao (str/upper-case icao)

        destination-dir (io/file nasr-path)
        airac (airac-data)
        zip-file (nasr/locate-zip airac destination-dir)

        airport (build-airport zip-file icao)]
    (pprint airport)))

(def ^:private cli-table
  [{:cmds ["build-airport"] :fn build-airport-cli
    :exec-args {:nasr-path "."}
    :args->opts [:icao]}])

(defn -main [& args]
  (cli/dispatch cli-table args))

(comment
  (-main "build-airport" "kjfk"))
