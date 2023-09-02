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
          :flex-direction :row
          :width (px 800)}])

(defn system-status []
  [:div (system-status-attrs)
   (when-let [weather (<sub [:game/weather])]
     [:<>
      [:div.row [:span (:date-time weather)] [:span (:altimeter weather)]]
      ; TODO Flow (?)

      [:div.row
       [:span "B"] ; TODO atis code
       [:span (str (:wind-heading weather)
                   "/"
                   (:wind-kts weather))]

       (when-let [{:keys [arrivals departures]} (<sub [:game/active-runways])]
         [:<>
          [:span "RWYS"]
          (into [:span] arrivals)
          " / "
          (into [:span] departures)])]])])
