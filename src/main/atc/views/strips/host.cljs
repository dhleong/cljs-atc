(ns atc.views.strips.host
  (:require
   [archetype.util :refer [<sub]]
   [atc.components.browser-window :refer [browser-window]]
   [atc.views.strips.subs :as subs]
   [atc.views.strips.view :refer [flight-strips]]))

(defn flight-strips-host []
  (when (= :popped-out (<sub [::subs/state]))
    [browser-window {:window-name "flight-strips"
                     :width 800
                     :height 600}
     [flight-strips]]))
