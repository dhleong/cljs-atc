(ns atc.views.game.graphics.aircraft
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]))

(def label-style (TextStyle. #js {:fill "#00ff00" ; TODO
                                  :fontSize 12}))

(def tracked-aircraft-style (TextStyle. #js {:fill "#1f478f"
                                             :fontSize 8}))

(defn- tracked [callsign]
  ; TODO: Adjust label location?
  [:<>
   [:> px/Text {:text "⬤"
                :anchor 0.5
                :x 0
                :y 0
                :style tracked-aircraft-style}]
   (when callsign
     [:> px/Text {:text callsign
                  :anchor 0.5
                  :x 0
                  :y 20
                  :style label-style}])])

(defn entity [{:keys [callsign]}]
  ; TODO render different graphics based on aircraft state
  [tracked callsign])

(defn entity-historical [_entity]
  [:> px/Container {:alpha 0.3}
   [entity nil]])

