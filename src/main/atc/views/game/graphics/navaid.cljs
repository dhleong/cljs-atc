(ns atc.views.game.graphics.navaid
  (:require
    ["@inlet/react-pixi" :as px]
    ["pixi.js" :refer [TextStyle]]))

(def label-style (TextStyle. #js {:fill "#f4f7ff"
                                  :fontSize 11}))

(defn entity [{:keys [id]}]
  [:> px/Container {:alpha 0.4}
   [:> px/Text {:anchor 0.5
                :x 0
                :y 0
                :style label-style
                :text "▲"}]
   [:> px/Text {:anchor 0.5
                :x 0
                :y 10
                :style label-style
                :text id}]])
