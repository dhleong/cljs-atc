(ns atc.views.game.graphics.navaid
  (:require
    ["@inlet/react-pixi" :as px]
    ["pixi.js" :refer [TextStyle]]))

(def label-style (TextStyle. #js {:fill "#00ff00" ; TODO
                                  :fontSize 12}))

(defn entity [{:keys [id]}]
  [:> px/Text {:anchor 0.5
               :x 0
               :y 0
               :style label-style
               :text id}])
