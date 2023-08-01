(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   [archetype.util :refer [<sub]]
   [atc.events :as events]
   [atc.views.game-setup :as game-setup]
   [atc.views.game.controls :refer [game-controls]]
   [atc.views.game.graphics.aircraft :as aircraft]
   [atc.views.game.graphics.navaid :as navaid]
   [atc.views.game.stage :refer [stage]]
   [atc.views.game.viewport :refer [viewport]]
   [atc.views.pause-screen :as pause-screen]
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
  (let [scale-atom (r/atom 1)
               set-scale! #(reset! scale-atom (/ 1 %))]
    (fn []

      ; Ensure we keep engine-injected subscriptions active
      (doseq [sub events/injected-subscriptions]
        (<sub sub))

      (let [entity-scale @scale-atom]
        [stage
         ; NOTE: The max world size should *maybe* be based on the airport?
         [viewport {:plugins ["drag" "pinch" "wheel"]
                    :pinch {:center {:x 0 :y 0}}
                    :wheel {:center {:x 0 :y 0}}
                    :center {:x 0 :y 0}
                    :on-scale set-scale!
                    :world-width default-world-dimension
                    :world-height default-world-dimension}

          [all-aircraft entity-scale]
          [all-navaids entity-scale]]]))))

(defn view []
  (if-not (<sub [:game/started?])
    [game-setup/view]

    [:<>
     [game]

     (when (<sub [:game/paused?])
       [pause-screen/view])

     [:div (game-controls-container-attrs)
      [game-controls]]]))
