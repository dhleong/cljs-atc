(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   [archetype.util :refer [<sub]]
   [atc.views.game.controls :refer [game-controls]]
   [atc.views.game.graphics.aircraft :as aircraft]
   [atc.views.game.graphics.navaid :as navaid]
   [atc.views.game.stage :refer [stage]]
   [atc.views.game.viewport :refer [viewport]]
   [reagent.core :as r]
   [spade.core :refer [defattrs]]))

; Basically a 3x a ~100 km CTR controller's radius, I guess
(def default-world-dimension (* 3 100 1000))

(defattrs game-controls-container-attrs []
  {:position :absolute
   :bottom 0
   :left 0
   :right 0})

(defn- positioner [scale entity & children]
  (let [position (:position entity)
        x (:x entity (:x position))
        y (:y entity (:y position))]
    (into [:> px/Container {:x x :y y :scale scale}] children)))

(defn- entities-renderer [{:keys [scale key-fn render] subscription :<sub
                           :or {key-fn :id}}]
  [:<>
   (for [entity (<sub subscription)]
     ^{:key (key-fn entity)}
     [positioner scale entity
      [render entity]])])

(defn- all-aircraft [scale]
  [:<>
   [entities-renderer {:scale scale
                       :<sub [:game/aircraft-historical]
                       :key-fn (juxt :history-n :callsign)
                       :render aircraft/entity-historical}]
   [entities-renderer {:scale scale
                       :<sub [:game/aircraft]
                       :key-fn :callsign
                       :render aircraft/entity}]])

(defn- all-navaids [scale]
  [entities-renderer {:scale scale
                      :<sub [:game/airport-navaids]
                      :render navaid/entity}])

(defn- game []
  (r/with-let [scale-atom (r/atom 1)
               set-scale! #(reset! scale-atom (/ 1 %))]
    (let [entity-scale @scale-atom]
      [stage
       ; NOTE: The max world size should *maybe* be based on the airport?
       [viewport {:plugins ["drag" "pinch" "wheel"]
                  :center {:x 0 :y 0}
                  :on-scale set-scale!
                  :world-width default-world-dimension
                  :world-height default-world-dimension}

        [all-aircraft entity-scale]
        [all-navaids entity-scale]]])))

(defn view []
  ; NOTE: These are ugly hacks because react gets mad on the first render for... some reason.
  ; See: https://github.com/inlet/react-pixi/issues/337
  (r/with-let [ready? (r/atom false)
               _ (js/setTimeout #(reset! ready? true) 0)]
    (when @ready?
      [:<>
       [game]
       [:div (game-controls-container-attrs)
        [game-controls]]])))
