(ns atc.styles.css
  (:require
   [garden.stylesheet :refer [cssfn]]))

(def linear-gradient (cssfn "linear-gradient"))
(def color-mix (cssfn "color-mix"))

(defn with-opacity [color-var opacity-perc]
  (color-mix [:in :srgb] [color-var (str (* 100 opacity-perc) "%")] :transparent))
