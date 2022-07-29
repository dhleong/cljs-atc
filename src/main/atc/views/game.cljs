(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [atc.views.game.stage :refer [stage]]
   [atc.views.game.viewport :refer [viewport]]
   [reagent.core :as r]))

(def text-style (TextStyle. #js {:fill "#ffffff"}))

(defn- game []
  [stage
   [viewport
    [:> px/Text {:text "Hey"
                 :anchor 0.5
                 :x 150
                 :y 150
                 :style text-style}]]])

(defn view []
  ; NOTE: These are ugly hacks because react gets mad on the first render for... some reason.
  (r/with-let [ready? (r/atom false)
               _ (js/setTimeout #(reset! ready? true) 0)]
    (when @ready?
      [game])))
