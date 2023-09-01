(ns atc.weather.api
  (:require
   [atc.weather.metar :as metar]
   [promesa.core :as p]))

(def ^:private metar-endpoint
  "https://metar.vatsim.net/metar.php?id=")

(defn fetch-metar-text [airport-icao]
  (js/fetch (str metar-endpoint airport-icao)))

(defn fetch-weather [airport-icao]
  (p/let [text (fetch-metar-text airport-icao)]
    (metar/parse-text text)))
