(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   [archetype.util :refer [<sub]]
   [atc.events :as events]
   [atc.styles :refer [full-screen]] ; [atc.theme :as theme]
   [atc.theme :as theme]
   [atc.views.game-setup :as game-setup]
   [atc.views.game.controls :refer [game-controls]]
   [atc.views.game.graphics.aircraft :as aircraft]
   [atc.views.game.graphics.center-facility :as center-facility]
   [atc.views.game.graphics.navaid :as navaid]
   [atc.views.game.graphics.polygon :refer [polygon]]
   [atc.views.game.graphics.runway :as runway]
   [atc.views.game.stage :refer [stage]]
   [atc.views.game.viewport :refer [viewport]]
   [atc.views.pause-screen :as pause-screen]
   [reagent.core :as r]
   [spade.core :refer [defattrs]]))

; Basically a 3x a ~100 km CTR controller's radius, I guess
(def default-world-dimension (* 3 100 1000))

(defattrs game-controls-container-attrs [{:keys [paused?]}]
  {:position :absolute
   :bottom 0
   :left 0
   :right 0
   :visibility (when paused? :hidden)})

(defn- positioner [scale entity & children]
  (let [position (:position entity)
        x (:x entity (:x position 0))
        y (:y entity (:y position 0))]
    (into [:> px/Container {:x x :y y :scale scale}] children)))

(defn- entities-renderer [{:keys [scale key-fn render] subscription :<sub
                           :or {key-fn :id}}]
  [:<>
   (for [entity (<sub subscription)]
     ^{:key (key-fn entity)}
     [positioner scale entity
      [render entity]])])

(defn airspace-geometry []
  [:<>
   (for [{:keys [id points]} (<sub [:game/airport-polygons])]
     ^{:key id}
     [polygon #js {:points points
                   :color theme/map-airspace-boundaries-int}])])

(defn all-neighboring-sectors [entity-scale]
  [entities-renderer {:scale entity-scale
                      :<sub [:game/neighboring-centers]
                      :key-fn :id
                      :render center-facility/entity}])

(defn all-runways []
  [entities-renderer {:scale 1
                      :<sub [:game/runways]
                      :key-fn :start-id
                      :render runway/entity}])

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

        [airspace-geometry]
        [all-neighboring-sectors entity-scale]
        [all-runways]
        [all-aircraft entity-scale]
        [all-navaids entity-scale]]])))

(defattrs game-container-attrs []
  {:composes (full-screen {:position :fixed})
   :height "100vh"})

(defn view []
  (let [paused? (<sub [:game/paused?])]
    (if-not (<sub [:game/started?])
      [game-setup/view]

      [:div (game-container-attrs)
       [game]

       [:div (game-controls-container-attrs {:paused? paused?})
        [game-controls]]

       (when paused?
         [pause-screen/view])])))
