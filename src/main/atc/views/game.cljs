(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [archetype.util :refer [<sub >evt]]
   [atc.views.game.graphics.aircraft :as aircraft]
   [atc.views.game.stage :refer [stage]]
   [atc.views.game.viewport :refer [viewport]]
   [reagent.core :as r]
   [spade.core :refer [defattrs]]))

(def text-style (TextStyle. #js {:fill "#ffffff"}))

(defattrs game-controls-attrs []
  {:display :flex
   :flex-direction :row
   :position :absolute
   :bottom 0
   :left 0
   :right 0})

(defn- game-controls []
  [:div (game-controls-attrs)
    [:button {:on-click #(>evt [:game/init])}
     "Init Game"]
    [:button {:on-click #(>evt [:game/reset])}
     "End Game"]

    (when-let [time-scale (<sub [:game/time-scale])]
      (if (= 0 time-scale)
        [:button {:on-click #(>evt [:game/set-time-scale 1])}
         "Resume"]
        [:button {:on-click #(>evt [:game/set-time-scale 0])}
         "Pause"]))])

(defn- aircraft-entity [{:keys [callsign position]}]
  (let [{:keys [x y]} position]
    (println "Draw aircraft " callsign " @ " x y)
    [aircraft/tracked callsign x y]))

(defn- game []
  (let [all-aircraft (<sub [:game/aircraft])]
    [stage
     [viewport
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

      (for [a all-aircraft]
        ^{:key (:callsign a)}
        [aircraft-entity a])]]))

(defn view []
  ; NOTE: These are ugly hacks because react gets mad on the first render for... some reason.
  (r/with-let [ready? (r/atom false)
               _ (js/setTimeout #(reset! ready? true) 0)]
    (println "mount game")
    (when @ready?
      [:<>
       [game]
       [game-controls]])))
