(ns atc.views.game.graphics.navaid
  (:require
   ["@inlet/react-pixi" :as px]
   ["pixi.js" :refer [TextStyle]]
   [archetype.util :refer [>evt]]
   [atc.theme :as theme]
   [reagent.core :as r]))

(defn- on-identify [id]
  (>evt [:help/identify-navaid id]))

(def label-style (TextStyle. #js {:fill theme/text
                                  :fontSize 11}))

(defn entity [{:keys [id]}]
  (r/with-let [handle-identify (partial on-identify id)]
    [:> px/Container {:alpha theme/map-label-alpha
                      :interactive true
                      :cursor :pointer
                      :click handle-identify
                      :rightclick handle-identify}
     [:> px/Text {:anchor 0.5
                  :x 0
                  :y 0
                  :style label-style
                  :text "▲"}]
     [:> px/Text {:anchor 0.5
                  :x 0
                  :y 10
                  :style label-style
                  :text id}]]))
