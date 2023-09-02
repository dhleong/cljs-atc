(ns atc.weather.spec
  (:require
   [spec-tools.data-spec :as ds]))

(def weather-spec
  (ds/spec
    {:name ::weather
     :spec {:altimeter string?
            :date-time string?
            :wind-heading number?
            :wind-kts number?}}))

; TODO: Should this belong to the airport?
(def default-wx
  {:altimeter "30.26"
   :date-time "012151Z"
   :wind-heading 300
   :wind-kts 4})
