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
          (>evt [:weather/fetched airport-icao wx]))
        (p/catch (fn [e]
                   ; TODO
                   (println e))))))
