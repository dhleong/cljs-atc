(ns atc.views.game.graphics.data-block-positioning
  (:require
   ["@inlet/react-pixi" :as px]
   ["@pixi/math-extras"]
   ["pixi.js" :refer [Point]]
   [applied-science.js-interop :as j]
   [atc.theme :as theme]
   [atc.views.game.graphics.line :refer [line]]
   [clojure.math :refer [atan2 round]]
   [reagent.core :as r]))

(def ^:private angle-interval (/ js/Math.PI 4))
(def ^:private angle-vectors
  {0 (Point. 1 0)
   1 (.normalize (Point. 1 1))
   2 (Point. 0 1)
   3 (.normalize (Point. -1 1))
   4 (Point. -1 0)
   -4 (Point. -1 0)
   -3 (.normalize (Point. -1 -1))
   -2 (Point. 0 -1)
   -1 (.normalize (Point. 1 -1))})

(def ^:private angle-pivots
  {0 (Point. 0 0)
   1 (Point. 0 0)
   2 (Point. 0.5 -0.2)
   3 (Point. 0.2 0.2)
   4 (Point. 0.2 0)
   -4 (Point. 0.2 0)
   -3 (Point. 0.2 -0.2)
   -2 (Point. 0.5 0.2)
   -1 (Point. 0 0)})

(def ^:private angle-offset-mod
  {0 0.5
   1 0.5
   2 0.5
   3 0
   4 0
   -4 0
   -3 0
   -2 0.5
   -1 0.5})

(def ^:private angle-line-length-mod
  {0 10
   1 10
   2 10
   3 38
   4 48
   -4 48
   -3 38
   -2 10
   -1 10})

(defn- handle-drag [state e]
  (cond-> state
    (:dragging? state)
    (as-> s
      (let [mouse-x (j/get-in e [:data :global :x])
            mouse-y (j/get-in e [:data :global :y])
            ref-x (j/get (:reference s) :x)
            ref-y (j/get (:reference s) :y)
            angle (atan2 (- mouse-y ref-y)
                         (- mouse-x ref-x))
            rounded-angle (round (/ angle angle-interval))
            block-bounds (-> (:block s)
                             (j/call :getBounds))
            block-offset (-> block-bounds
                             (j/get :width)
                             (* (get angle-offset-mod rounded-angle)))]
        (assoc s
               :length (-> (.subtract
                             (j/get-in e [:data :global])
                             (:reference s))
                           (.magnitude)
                           (abs)
                           (- block-offset)
                           (max 10)
                           (min 200))
               :width (j/get block-bounds :width)
               :angle rounded-angle)))))

(defn- compute-positions [{:keys [angle] :as state}]
  (let [angle-vec (get angle-vectors angle)
        line-start (.multiplyScalar angle-vec 10)
        container-offset-length (.multiplyScalar angle-vec (:length state))
        container-pos (.add line-start container-offset-length)
        line-end (.subtract
                   container-pos
                   (.multiplyScalar angle-vec
                                    (get angle-line-length-mod angle)))
        pivot (.multiplyScalar
                (get angle-pivots angle)
                (:width state))]
    {:line-start line-start
     :line-end line-end
     :container-pos container-pos
     :pivot pivot}))

(defn data-block-positioning [{:keys [tracked?]} block]
  (r/with-let [datablock-state (r/atom {:dragging? false
                                        :block-offset 0
                                        :angle 0
                                        :width 0
                                        :length 10})
               on-down (fn [e]
                         (.stopPropagation e)
                         (swap!
                           datablock-state assoc
                           :dragging? true
                           :block (j/get e :target)
                           :reference (j/call-in e [:target :parent
                                                    :getGlobalPosition])))
               on-up (fn [e]
                       (when (:dragging? @datablock-state)
                         (.stopPropagation e)
                         (swap! datablock-state assoc :dragging? false)))
               on-move (fn [e]
                         (swap! datablock-state handle-drag e))]
    (let [{:keys [line-start line-end
                  pivot container-pos]} (compute-positions @datablock-state)]
      [:<>
       [line {:from line-start
              :to line-end
              :color (if tracked?
                       0xffffff
                       theme/aircraft-untracked-obj-int)}]

       [:> px/Container {:interactive true
                         :cursor :grab
                         :pointerdown on-down
                         :pointerup on-up
                         :pointerupoutside on-up
                         :pointermove on-move
                         :pivot (or pivot js/undefined)
                         :x (j/get container-pos :x)
                         :y (j/get container-pos :y)}
        block]])))
