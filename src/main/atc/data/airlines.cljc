(ns atc.data.airlines
  (:require
   [atc.voice.parsing.callsigns :refer [airline-names]]))

#_(def all-airlines
  {"AAL" {:callsign "AAL"
          :radio-name "american"}
   "BAW" {:callsign "BAW"
          :radio-name "speed bird"}
   "DAL" {:callsign "DAL"
          :radio-name "delta"}
   "JBU" {:callsign "JBU"
          :radio-name "jet blue"}
   "RPA" {:callsign "RPA"
          :radio-name "brickyard"}
   "SWA" {:callsign "SWA"
          :radio-name "south west"}})

(def all-airlines
  (->> airline-names
       (into {}
             (map (fn [[radio-name id]]
                    [id {:callsign radio-name
                         :radio-name radio-name}])))))
