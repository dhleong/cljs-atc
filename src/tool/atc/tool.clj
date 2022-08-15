(ns atc.tool
  (:require
   [atc.nasr :as nasr]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]))

(defn compose-runways [data]
  (mapv
    (fn [rwy]
      {:start-id (:base-end-id rwy)
       :start-threshold [(:base-end-latitude rwy) (:base-end-longitude rwy)]
       :end-id (:reciprocal-end-id rwy)
       :end-threshold [(:reciprocal-end-latitude rwy) (:reciprocal-end-longitude rwy)]})
    (:rwy data)))

(defn build-airport [in icao]
  (let [{:keys [apt] :as data} (time (nasr/find-airport-data in icao))
        runways (compose-runways data)]

    {:id (:icao apt)
     :name (:name apt)
     :magnetic-north "TODO"
     :position "TODO"
     :runways runways}))

(defn -main []
  (let [path "/Users/daniel/Downloads/28DaySubscription_Effective_2022-07-14/APT.txt"]
    (with-open [reader (io/input-stream path)]
      (pprint (build-airport reader "KJFK"))
      #_(println (build-airport reader "PFAK")))))
