(ns atc.views.game.graphics.aircraft
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]))

(def label-style (TextStyle. #js {:fill "#00ff00" ; TODO
                                  :fontSize 8}))

(def tracked-aircraft-style (TextStyle. #js {:fill "#1f478f"
                                             :fontSize 8}))


(defn tracked [callsign x y]
  [:<>
   [:> px/Text {:text "⬤"
                :anchor 0.5
                :x x
                :y y
                :style tracked-aircraft-style}]
   [:> px/Text {:text callsign
                :anchor 0.5
                :x (+ x 10) ; FIXME ?
                :y (+ y 10)
                :style label-style}]])
