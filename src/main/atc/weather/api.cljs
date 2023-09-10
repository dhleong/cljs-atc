(ns atc.weather.api
  (:require
   [atc.util.fetch :refer [fetch-with-timeout]]
   [atc.weather.metar :as metar]
   [promesa.core :as p]))

(def ^:private metar-endpoint
  "https://metar.vatsim.net/metar.php?id=")

(defn fetch-metar-text [airport-icao]
  (fetch-with-timeout :text (str metar-endpoint airport-icao)))

(defn fetch-weather [airport-icao]
  (p/let [text (fetch-metar-text airport-icao)]
    (or (metar/parse-text text)
        (throw (ex-info "Failed to parse METAR" {:text text
                                                 :icao airport-icao})))))
