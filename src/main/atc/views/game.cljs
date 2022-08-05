(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [archetype.util :refer [<sub]]
   [atc.views.game.controls :refer [game-controls]]
   [atc.views.game.graphics.aircraft :as aircraft]
   [atc.views.game.stage :refer [stage]]
   [atc.views.game.viewport :refer [viewport]]
   [reagent.core :as r]
   [spade.core :refer [defattrs]]))

(def text-style (TextStyle. #js {:fill "#ffffff"}))

(defattrs game-controls-container-attrs []
  {:position :absolute
   :bottom 0
   :left 0
   :right 0})

(defn- all-aircraft [scale-atom]
  [:<>
   (let [scale (/ 1 @scale-atom)]
     (for [a (<sub [:game/aircraft])]
       ^{:key (:callsign a)}
       [aircraft/entity {:scale scale} a]))])

(defn- game []
  (r/with-let [scale-atom (r/atom 1)
               set-scale! (partial reset! scale-atom)]
    [stage
     [viewport {:plugins ["drag" "pinch" "wheel"]
                :on-scale set-scale!}
      (let [voice-state (<sub [:voice/state])]
        [:> px/Text {:text (or voice-state "Hey")
                     :anchor 0
                     :x 50
                     :y 50
                     :style text-style}])

      (when-let [partial-text (<sub [:voice/partial])]
        [:> px/Text {:text partial-text
                     :anchor 0
                     :x 50
                     :y 150
                     :style text-style}])

      [all-aircraft scale-atom]]]))

(defn view []
  ; NOTE: These are ugly hacks because react gets mad on the first render for... some reason.
  (r/with-let [ready? (r/atom false)
               _ (js/setTimeout #(reset! ready? true) 0)]
    (when @ready?
      [:<>
       [game]
       [:div (game-controls-container-attrs)
        [game-controls]]])))
