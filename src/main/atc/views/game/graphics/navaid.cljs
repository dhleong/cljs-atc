(ns atc.views.game.graphics.navaid
  (:require
    ["@inlet/react-pixi" :as px]
    ["pixi.js" :refer [TextStyle]]))

(def label-style (TextStyle. #js {:fill "#f4f7ff"
                                  :fontSize 11}))

(defn entity [{:keys [id]}]
  [:> px/Text {:anchor 0.5
               :alpha 0.4
               :x 0
               :y 0
               :style label-style
               :text id}])
