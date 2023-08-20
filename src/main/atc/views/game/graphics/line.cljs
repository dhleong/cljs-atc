(ns atc.views.game.graphics.line
  (:require
   ["@inlet/react-pixi" :as px]
   [applied-science.js-interop :as j]))

(defn- unpack-coord [v]
  (if (map? v)
    [(:x v 0)
     (:y v 0)]
    [(j/get v :x 0)
     (j/get v :y 0)]))

(defn line [{:keys [from to width color alpha]
             :or {width 1
                  alpha 1}}]
  (when-not (int? color)
    (js/console.warn ":color should be an int!"))
  [:> px/Graphics {:draw (fn draw [^js g]
                           (let [[fx fy] (unpack-coord from)
                                 [tx ty] (unpack-coord to)]
                             (doto g
                               (.clear)
                               (.lineStyle width color alpha)
                               (.moveTo fx fy)
                               (.lineTo tx ty))))}])
