(ns atc.views.game.graphics.aircraft
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [atc.data.units :as units]
   [atc.views.game.graphics.line :refer [line]]
   [clojure.math :refer [floor]]
   [clojure.string :as str]))

(def tracked-label-style (TextStyle. #js {:fill "#ffffff"
                                          :fontFamily "monospace"
                                          :fontSize 12}))

(def tracked-aircraft-style (TextStyle. #js {:fill "#1f478f"
                                             :fontSize 8}))

(def untracked-aircraft-style (TextStyle. #js {:fill "#00ff00"
                                               :fontSize 8}))

; ======= Data formatting =================================

(defn- format-data-block-line [line]
  ; FIXME
  (str/join "  " line))

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

   [:> px/Container {:x 55 :y 0}
    block]])

(defn- tracked-position-symbol []
  [:> px/Text {:text "⬤"
               :anchor 0.5
               :x 0
               :y 0
               :style tracked-aircraft-style}])

(defn- full-data-block [{:keys [callsign position speed]}]
  (let [alt-hundreds-ft (format-altitude (:z position))

        line1 [callsign]
        line2 [alt-hundreds-ft speed]]
    [:> px/Text {:text (str (format-data-block-line line1)
                            "\n"
                            (format-data-block-line line2))
                 :anchor 0.5
                 :x 0
                 :style tracked-label-style}]))

(defn- tracked [craft]
  ; TODO: Adjust label location?
  [:<>
   [tracked-position-symbol]

   [data-block-positioning craft
    [full-data-block craft]]])

(defn- untracked [{{altitude :z} :position :as craft}]
  [:<>
   [:> px/Text {:text "*"
                :anchor 0.5
                :x 0
                :y 0
                :style untracked-aircraft-style}]
   [data-block-positioning craft
    [:> px/Text {:text (format-altitude altitude)
                 :style untracked-aircraft-style}]]])


; ======= Public interface ================================

(defn entity [{:keys [callsign] :as craft}]
  ; TODO render different graphics based on aircraft state
  ; TODO "tracked" vs "untracked" state
  (if callsign
    [tracked craft]
    [untracked craft]))

(defn entity-historical [_entity]
  [:> px/Container {:alpha 0.3}
   [tracked-position-symbol]])

