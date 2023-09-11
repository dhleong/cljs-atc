(ns atc.views.strips.view
  (:require
   [archetype.util :refer [<sub >evt]]
   [atc.views.strips.events :as events]
   [atc.views.strips.subs :as subs]
   [garden.units :refer [px]]
   [goog.string :as gstring]
   [spade.core :refer [defattrs]]))

(defn- pop-in-button []
  (when (= :popped-out (<sub [::subs/state]))
    [:button {:on-click #(>evt [::events/set-state :hidden])}
     "Pop in"]))

(defattrs flight-strip-attrs []
  {:display :flex
   :font-family :monospace
   :width :100%
   :height (px 70)
   :cursor :default}

  [:.aircraft-identification :.col2 :.col3 :.route
   {:display :flex
    :flex-direction :column
    :justify-content :space-between
    :height :100%
    :padding (px 4)}])

(defn- flight-strip-form [{:keys [callsign config
                                  col2 col3
                                  route
                                  table]}]
  [:li (flight-strip-attrs)
   [:div.aircraft-identification
    [:div.callsign callsign]
    [:div.craft-type (:type config)]
    [:div.blank (gstring/unescapeEntities "&nbsp;")]]

   [:div.col2
    col2]

   [:div.col3
    col3]

   [:div.route
    route]

   [:div.table
    ; TODO
    (str table)]])

(defn- flight-strip [{:keys [callsign config arrival?] :as strip}]
  (if arrival?
    [flight-strip-form
     {:callsign callsign
      :config config
      :col2 nil ; TODO entry fix
      :route [:<> "TODO altitudes"]}]

    ; Departure:
    [flight-strip-form
     {:callsign callsign
      :config config
      :col2 nil ; TODO cruise flight level
      :col3 [:<>
             [:div.origin (:origin strip)]
             [:div.departure-fix (:departure-fix strip)]]
      :route (:route strip)}]))

(defn- flight-strip-group [& {subscription :<sub :keys [title]}]
  (let [strips (<sub subscription)]
    [:<>
     [:h2.strips-group-title title]
     [:ul.strips-group
      (for [strip strips]
        ^{:key (:callsign strip)}
        [flight-strip strip])]]))

(defattrs flight-strips-attrs []
  {:background :*background-secondary*
   :color :*text*
   :cursor :default
   :height :100%
   :overflow-y :auto
   :user-select :none}
  [:.strips-group {:padding 0
                   :list-style-type :none}])

(defn flight-strips []
  [:div (flight-strips-attrs)
   [pop-in-button]

   ; TODO Arrival strips should be grouped by runway
   [flight-strip-group
    :title "Arrivals"
    :<sub [::subs/arrival-strips]]

   [flight-strip-group
    :title "Departures"
    :<sub [::subs/departure-strips]]])
