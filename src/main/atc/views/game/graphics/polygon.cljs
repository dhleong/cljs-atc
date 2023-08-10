(ns atc.views.game.graphics.polygon
  (:require
   ["@inlet/react-pixi" :as px]
   [applied-science.js-interop :as j]
   [reagent.core :as r]))

(defn- draw-points [^js g points]
  (let [start (first points)]
    (.moveTo g (:x start 0) (:y start 0))
    (doseq [p (next points)]
      (.lineTo g (:x p) (:y p)))
    (.lineTo g (:x start 0) (:y start 0))))

(j/defn static-polygon [^:js {:keys [points width color alpha]
                              :or {width 1000
                                   alpha 1}}]
  (when-not (int? color)
    (js/console.warn ":color should be an int!"))
  (r/with-let [draw (fn draw [^js g]
                      (doto g
                        (.clear)
                        (.lineStyle width color alpha)
                        (.beginFill color)
                        (draw-points points)))]
    [:> px/Graphics {:draw draw}]))
