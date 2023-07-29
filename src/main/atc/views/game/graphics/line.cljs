(ns atc.views.game.graphics.line
  (:require
   ["@pixi/react" :as px]))

(defn line [{:keys [from to width color alpha]
             :or {width 1
                  alpha 1}}]
  (when-not (int? color)
    (js/console.warn ":color should be an int!"))
  [:> px/Graphics {:draw (fn draw [^js g]
                           (doto g
                             (.clear)
                             (.lineStyle width color alpha)
                             (.moveTo (:x from 0) (:y from 0))
                             (.lineTo (:x to) (:y to))))}])
