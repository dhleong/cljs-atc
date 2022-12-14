(ns atc.views.game.stage
  (:require
   ["@inlet/react-pixi" :as px]))

(defn stage [& children]
  (into [:> px/Stage {:width js/window.innerWidth
                      :height js/window.innerHeight
                      :raf false
                      :render-on-component-change true
                      :options {:auto-density true
                                :resize-to js/window}}]
        children))
