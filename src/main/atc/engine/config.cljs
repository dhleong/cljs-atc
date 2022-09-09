(ns atc.engine.config
  (:require
   [atc.data.units :as units]))

(defrecord AircraftConfig [turn-rate climb-rate descent-rate])

(defn- ft-m->m-s [ft-m]
  (/ (units/ft->m ft-m) 60))

(defn create [kvs]
  (-> kvs

      ; Input for these is ft/minute; convert to m/s
      (update :climb-rate ft-m->m-s)
      (update :descent-rate ft-m->m-s)

      (map->AircraftConfig)))

