(ns atc.weather.spec
  (:require
   [spec-tools.data-spec :as ds]))

(def weather-spec
  (ds/spec
    {:name ::weather
     :spec {:wind-heading number?
            :wind-kts number?}}))

; TODO: Should this belong to the airport?
(def default-wx
  {:wind-heading 300
   :wind-kts 4})
