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
            rounded-angle (round (/ angle angle-interval))]
        (assoc s
               :length (-> (.subtract
                             (j/get-in e [:data :global])
                             (:reference s))
                           (.magnitude)
                           (abs)
                           (max 10))
               :angle (get angle-vectors rounded-angle))))))

(defn data-block-positioning [{:keys [tracked?]} block]
  (r/with-let [datablock-state (r/atom {:dragging? false
                                        :angle (get angle-vectors 0)
                                        :length 10})
               on-down (fn [e]
                         (.stopPropagation e)
                         (swap!
                           datablock-state assoc
                           :dragging? true
                           :reference (j/call-in e [:target :parent
                                                    :getGlobalPosition])))
               on-up (fn [e]
                       (when (:dragging? @datablock-state)
                         (.stopPropagation e)
                         (swap! datablock-state assoc :dragging? false)))
               on-move (fn [e]
                         (swap! datablock-state handle-drag e))]
    (let [{:keys [^js angle] :as state} @datablock-state
          line-start (.multiplyScalar angle 10)
          line-length (.multiplyScalar angle (:length state))
          line-end (.add line-start line-length)
          container-pos (.add line-end line-start)]
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
                         :x (j/get container-pos :x)
                         :y (j/get container-pos :y)}
        block]])))
