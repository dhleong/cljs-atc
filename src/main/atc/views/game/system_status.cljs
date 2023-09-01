(ns atc.views.game.system-status
  (:require
   [archetype.util :refer [<sub]]
   [garden.units :refer [px]]
   [spade.core :refer [defattrs]]))

(defattrs system-status-attrs []
  {:color :*map-text*
   :font-family :monospace
   :font-weight 600
   :position :absolute
   :padding (px 32)}

  [:.row {:display :flex
          :gap (px 8)
          :flex-direction :row}])

(defn system-status []
  [:div (system-status-attrs)
   (when-let [weather (<sub [:game/weather])]
     ; TODO Time, altimeter
     ; TODO Flow (?)
     ; TODO ATIS code, wind direction/speed, (runways below)
     [:div (str weather)])

   (when-let [{:keys [arrivals departures]} (<sub [:game/active-runways])]
     ; TODO
     [:div.row "RWYS " (into [:span] arrivals) " / " (into [:span] departures)])])
