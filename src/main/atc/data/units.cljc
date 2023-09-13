(ns atc.data.units
  (:require
   [clojure.math :refer [floor]]))

(defn m->ft [meters]
  ; NOTE: We probably *never* need to do this except for displaying
  ; or verbally reporting an aircraft's altitude in feet, so let's
  ; just make it clean:
  (floor (* 3.28084 meters)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn m->nm [meters]
  (* 0.000539957 meters))

(defn ft->m [feet]
  (* 0.3048 feet))

(defn ft->fl [feet]
  (/ feet 100))

(defn nm->m [nautical-miles]
  (* 1852 nautical-miles))

(defn sm->m [statute-miles]
  (* 1609.34 statute-miles))
