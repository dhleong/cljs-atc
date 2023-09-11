(ns atc.views.strips.host
  (:require
   [archetype.util :refer [<sub >evt]]
   [atc.components.browser-window :refer [browser-window]]
   [atc.views.strips.events :as events]
   [atc.views.strips.subs :as subs]
   [atc.views.strips.view :refer [flight-strips]]
   [garden.units :refer [px]]
   [spade.core :refer [defattrs]]))

(def ^:private width 400)

(defattrs flight-strips-container-attrs [expanded?]
  {:display :flex
   :align-items :center
   :height :100%
   :pointer-events :none
   :position :absolute
   :right (if expanded? 0 (px (- width)))
   :transition [[:all "120ms" (if expanded?
                                :ease-in
                                :ease-out)]]}
  [:.controls {:display :flex
               :flex-direction :column
               :pointer-events :all}]
  [:.actual {:pointer-events :all
             :height :100%
             :width (px width)}])

(defn flight-strips-host []
  (let [state (<sub [::subs/state])]
    (if (= state :popped-out)
      [browser-window {:window-name "flight-strips"
                       :width width
                       :height 600}
       [flight-strips]]

      [:div (flight-strips-container-attrs (= state :expanded))
       [:div.controls
        [:button.interactive {:on-click #(>evt [::events/set-state :popped-out])}
         "^"]
        (case state
          :expanded
          [:button.interactive {:on-click #(>evt [::events/set-state :hidden])}
           ">>"]

          [:button.interactive {:on-click #(>evt [::events/set-state :expanded])}
           "<<"])]

       [:div.actual
        [flight-strips]]])))
