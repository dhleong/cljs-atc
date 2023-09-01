(ns atc.weather.spec
  (:require
   [spec-tools.data-spec :as ds]))

(def weather-spec
  (ds/spec
    {:name ::weather
     :spec {:wind-heading number?
            :wind-kts number?}}))
