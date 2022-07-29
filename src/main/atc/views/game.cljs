(ns atc.views.game
  (:require
   ["@inlet/react-pixi" :as px]
   [reagent.core :as r]))

(defn stage []
  [:> px/Stage {:width js/window.innerWidth
                :height js/window.innerHeight
                :options {:resize-to js/window}}])

(defn view []
  ; NOTE: These are ugly hacks because react gets mad on the first render for... some reason.
  (r/with-let [ready? (r/atom false)
               _ (js/setTimeout #(reset! ready? true) 0)]
    (when @ready?
      [stage])))
