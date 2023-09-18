(ns atc.weather.fx
  (:require
   [archetype.util :refer [>evt]]
   [atc.weather.api :as api]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]))

(reg-fx
  ::fetch
  (fn [airport-icao]
    (-> (p/let [wx (api/fetch-weather airport-icao)]
          (println "[wx] Fetched @" airport-icao ": " wx)
          (>evt [:weather/fetched airport-icao wx]))
        (p/catch (fn [e]
                   (js/console.error "[wx] Failed to fetch @"
                                     airport-icao e)
                   (when-some [text (:text (ex-data e))]
                     (js/console.error "[wx]  - METAR text: " text))
                   (>evt [:weather/failed airport-icao]))))))
