(ns atc.views.strips.host
  (:require
   [archetype.util :refer [<sub >evt]]
   [atc.components.browser-window :refer [browser-window]]
   [atc.components.icon :refer [icon]]
   [atc.components.icon-button :refer [icon-button]]
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
  [:.controls {:background-color :*background-secondary*
               :border-top-left-radius (px 4)
               :border-bottom-left-radius (px 4)
               :display :flex
               :flex-direction :column
               :padding (px 2)
               :pointer-events :all
               :user-select :none}]
  [:.actual {:pointer-events :all
             :height :100%
             :width (px width)}])

(defn flight-strips-host []
  (let [state (<sub [::subs/state])]
    (if (= state :popped-out)
      [browser-window {:window-name "flight-strips"
                       :on-close #(>evt [::events/set-state :hidden])
                       :width width
                       :height 600}
       [flight-strips]]

      [:div (flight-strips-container-attrs (= state :expanded))
       [:div.controls
        [icon-button {:on-click #(>evt [::events/set-state :popped-out])
                      :aria-label "Pop out flight strips into separate window"}
         (icon :open-in-new)]

        (case state
          :expanded
          [icon-button {:on-click #(>evt [::events/set-state :hidden])}
           (icon :right-panel-close)]

          [icon-button {:on-click #(>evt [::events/set-state :expanded])}
           (icon :right-panel-open)])]

       [:div.actual
        [flight-strips]]])))
