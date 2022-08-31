(ns atc.data.units 
  (:require
   [clojure.math :refer [floor]]))

(defn m->ft [meters]
  ; NOTE: We probably *never* need to do this except for displaying
  ; or verbally reporting an aircraft's altitude in feet, so let's
  ; just make it clean:
  (floor (* 3.28084 meters)))

(defn m->nm [meters]
  (* 0.000539957 meters))

(defn ft->m [feet]
  (* 0.3048 feet))

(defn nm->m [nautical-miles]
  (* 1852 nautical-miles))
