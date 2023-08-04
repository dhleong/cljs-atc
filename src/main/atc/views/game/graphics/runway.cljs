(ns atc.views.game.graphics.runway
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [atc.theme :as theme]
   [atc.views.game.graphics.line :refer [line]]))

(def label-style (TextStyle. #js {:fill theme/map-label-opaque
                                  :fontSize 120}))

(defn entity [{:keys [start-angle start-id start-threshold
                      end-angle end-id end-threshold]}]
  [:<>
   [:> px/Container {:x (:x start-threshold)
                     :y (:y start-threshold)
                     :angle start-angle}
    [:> px/Text {:text start-id
                 :anchor 0.5
                 :y 100
                 :style label-style}]]

   [:> px/Container {:x (:x end-threshold)
                     :y (:y end-threshold)
                     :angle end-angle}
    [:> px/Text {:text end-id
                 :anchor 0.5
                 :y 100
                 :style label-style}]]

   [line {:width 80
          :from start-threshold
          :to end-threshold
          :color theme/map-label-opaque-int}]])
