(ns atc.views.game.graphics.navaid
  (:require
    ["@pixi/react" :as px]
    ["pixi.js" :refer [TextStyle]]
    [atc.theme :as theme]))

(def label-style (TextStyle. #js {:fill theme/text
                                  :fontSize 11}))

(defn entity [{:keys [id]}]
  [:> px/Container {:alpha theme/map-label-alpha}
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
