(ns atc.tool
  (:require
   [atc.nasr :as nasr]
   [atc.nasr.airac :refer [airac-data]]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]))

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
        fixes (time (nasr/find-fixes
                      zip-file
                      :near position
                      :in-range (* 100 1000)
                      :on-charts #{:sid :star}))]

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

(defn -main []
  (let [destination-dir (io/file ".")
        airac (airac-data)
        zip-file (nasr/locate-zip airac destination-dir)

        airport (build-airport zip-file "KJFK")]
    (pprint airport)))
