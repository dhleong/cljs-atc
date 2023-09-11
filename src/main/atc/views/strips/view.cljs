(ns atc.views.strips.view
  (:require
    [archetype.util :refer [<sub >evt]]
    [atc.views.strips.events :as events]
    [spade.core :refer [defattrs]]))

(defattrs flight-strips-attrs []
  {:background "red"})

(defn flight-strips []
  [:div (flight-strips-attrs)
   [:button {:on-click #(>evt [::events/set-state :expanded])}
    "Pop in"]
   [:div.weather (str (<sub [:game/weather]))]])
