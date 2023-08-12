(ns atc.views.game.graphics.aircraft
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [archetype.util :refer [<sub]]
   [atc.data.units :as units]
   [atc.theme :as theme]
   [atc.views.game.graphics.line :refer [line]]
   [clojure.math :refer [floor]]
   [clojure.string :as str]))

(def tracked-label-style (TextStyle. #js {:fill theme/aircraft-tracked-label
                                          :fontFamily "monospace"
                                          :fontSize 12}))

(def tracked-aircraft-style (TextStyle. #js {:fill theme/aircraft-tracked-obj
                                             :fontSize 8}))

(def untracked-aircraft-style (TextStyle. #js {:fill theme/aircraft-untracked-obj
                                               :fontFamily "monospace"
                                               :fontSize 12}))

; ======= Data formatting =================================

(defn- format-data-block-line [line]
  ; TODO Should we trim to some length...?
  (str/join " " line))

(defn format-altitude [altitude-meters]
  (loop [s (str (-> altitude-meters
                    units/m->ft
                    (/ 100)
                    (floor)))]

    ; Pad-left with 0
    (if (>= 3 (count s))
      s
      (recur (str "0" s)))))


; ======= Rendering =======================================

(defn- data-block-positioning [_craft block]
  [:<>
   ; TODO choose color based on tracked state
   [line {:from {:x 10 :y 0}
          :to {:x 20 :y 0}
          :color 0xffffff}]

   [:> px/Container {:x 28 :y 0}
    block]])

(defn- tracked-position-symbol []
  [:> px/Text {:text "⬤"
               :anchor 0.5
               :x 0
               :y 0
               :style tracked-aircraft-style}])

(defn- build-datablock [airport mode
                        {:keys [config departure-fix position speed]}]
  (case mode
    :altitude/speed
    (let [alt-hundreds-ft (format-altitude (:z position))
          speed-tens-kts (js/Math.floor (/ speed 10))]
      [alt-hundreds-ft speed-tens-kts])

    :exit-fix/aircraft-type
    (let [exit-fix-code (get-in airport [:departure-fix-codes
                                         departure-fix])
          aircraft-type (get config :type)]
      [exit-fix-code aircraft-type])

    ["TODO" (str mode)]))

(defn- full-data-block [{:keys [callsign] :as aircraft}]
  (let [mode (<sub [:datablock-mode/full])
        airport (<sub [:game/airport])
        line1 [callsign]
        line2 (build-datablock airport mode aircraft)]
    [:> px/Text {:text (str (format-data-block-line line1)
                            "\n"
                            (format-data-block-line line2))
                 :anchor {:x 0 :y 0.5}
                 :style tracked-label-style}]))

(defn- tracked [craft]
  ; TODO: Adjust label location?
  [:<>
   [tracked-position-symbol]

   [data-block-positioning craft
    [full-data-block craft]]])

(defn- untracked [{{altitude :z} :position :as craft}
                  {:keys [track-symbol]}]
  [:<>
   [:> px/Text {:text (or track-symbol "＊")
                :anchor 0.5
                :style untracked-aircraft-style}]
   [data-block-positioning craft
    [:> px/Text {:text (format-altitude altitude)
                 :anchor {:x 0 :y 0.5}
                 :style untracked-aircraft-style}]]])


; ======= Public interface ================================

(defn entity [{:keys [callsign] :as craft}]
  (let [tracked-map (<sub [:game/tracked-aircraft-map])
        track (get tracked-map callsign)
        self-tracked? (:self? track)]
    ; TODO render different graphics based on aircraft state
    (if self-tracked?
      [tracked craft]
      [untracked craft track])))

(defn entity-historical [_entity]
  [:> px/Container {:alpha 0.3}
   [tracked-position-symbol]])

