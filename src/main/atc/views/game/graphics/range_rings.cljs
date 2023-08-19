(ns atc.views.game.graphics.range-rings
  (:require
   ["@inlet/react-pixi" :as px]
   [archetype.util :refer [<sub]]
   [atc.data.units :refer [nm->m]]
   [atc.theme :as theme]
   [react :as React]))

(defn- f>range-rings []
  (let [ranges (<sub [:ui/range-rings])
        ring-width 200
        draw (React/useCallback
               (fn [^js g]
                 (doto g
                   (.clear)
                   (.lineStyle ring-width theme/map-label-opaque-int 0.3))
                 (doseq [range-nm ranges]
                   (.drawCircle g 0 0 (nm->m range-nm))))
               #js [ring-width ranges])]
    [:> px/Graphics {:draw draw}]))

(defn range-rings [entity-scale]
  [:f> f>range-rings entity-scale])
