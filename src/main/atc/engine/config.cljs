(ns atc.engine.config
  (:require
   [atc.data.units :as units]))

(defrecord AircraftConfig [turn-rate climb-rate descent-rate])

(defn create [kvs]
  (-> kvs
      (update :climb-rate units/ft->m)
      (update :descent-rate units/ft->m)
      (map->AircraftConfig)))

