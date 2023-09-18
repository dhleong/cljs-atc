(ns atc.views.game.system-status
  (:require
   [archetype.util :refer [<sub]]
   [atc.components.help-span :refer [help-span]]
   [garden.units :refer [px]]
   [spade.core :refer [defattrs]]))

(defattrs system-status-attrs []
  {:color :*map-text*
   :cursor :default
   :font-family :monospace
   :font-weight 600
   :position :absolute
   :padding (px 32)
   :user-select :none}

  [:.row {:display :flex
          :gap (px 8)
          :flex-direction :row
          :width (px 800)}])

(defn system-status []
  [:div (system-status-attrs)
   (when-let [weather (<sub [:game/weather])]
     [:<>
      [:div.row
       [help-span :weather-time
        (:date-time weather)]
       [help-span :primary-altimeter
        (:altimeter weather)]
       [help-span :visibility-sm
        (:visibility-sm weather) "SM"]]

      ; TODO Flow (?)

      [:div.row
       [help-span :atis
        (:atis weather)]
       [help-span :wind
        ; TODO gusts
        (str (:wind-heading weather)
             "/"
             (:wind-kts weather))]

       (when-let [{:keys [arrivals departures]} (<sub [:game/active-runways])]
         [:<>
          [help-span :active-runways-primary
           "RWYS"]
          (into [help-span :active-arrival-runways-primary] arrivals)
          " / "
          (into [help-span :active-departurel-runways-primary] departures)])]])])
