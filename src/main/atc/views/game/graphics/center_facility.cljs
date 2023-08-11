(ns atc.views.game.graphics.center-facility
   (:require
    ["pixi.js" :refer [TextStyle]]
    ["@inlet/react-pixi" :as px]
    [atc.theme :as theme]))

(def frequency-style (TextStyle. #js {:align "center"
                                      :fill theme/text
                                      :fontFamily "monospace"

                                      :fontSize 12}))

(defn entity [{:keys [frequency label]}]
  [:> px/Text {:anchor 0.5
               :alpha 0.75
               :style frequency-style
               :text (str label "\n" frequency)}])
