(ns atc.views.strips.view
  (:require
   [archetype.util :refer [<sub >evt]]
   [atc.views.strips.events :as events]
   [atc.views.strips.subs :as subs]
   [garden.units :refer [px]]
   [goog.string :as gstring]
   [spade.core :refer [defattrs]]))

(def ^:private nbsp (gstring/unescapeEntities "&nbsp;"))

(defn- pop-in-button []
  (when (= :popped-out (<sub [::subs/state]))
    [:button {:on-click #(>evt [::events/set-state :hidden])}
     "Pop in"]))

(defattrs flight-strip-attrs []
  (let [column-border [[(px 1) :solid :*text*]]]
    [:&
     {:border [[(px 2) :outset :*map-text*]]
      :border-radius (px 2)
      :display :grid
      :grid-template-columns [["1fr" "1fr" "1fr" "3fr"]]
      :font-family :monospace
      :cursor :default}

     [:.aircraft-identification :.squawk-column :.col3 :.route
      {:display :flex
       :flex-direction :column
       :justify-content :space-between
       :padding [[(px 4) 0]]}]

     [:.aircraft-identification :.squawk-column :.col3
      {:text-align :center}]

     [:.squawk-column {:border-left column-border
                       :border-right column-border}
      [:.middle {:padding (px 4)}]
      [:.middle {:border-top column-border
                 :border-bottom column-border}]]

     [:.route {:border-left column-border
               :padding (px 4)}]]))

(defn- flight-strip-form [{:keys [callsign config
                                  col3
                                  route
                                  table]
                           [sc-mid sc-bottom] :squawk-column}]
  [:li (flight-strip-attrs)
   [:div.aircraft-identification
    [:div.callsign callsign]
    [:div.craft-type (:type config)]
    [:div.blank nbsp]]

   [:div.squawk-column
    [:div.squask nbsp] ; TODO
    [:div.middle (or sc-mid nbsp)]
    [:div.bottom (or sc-bottom nbsp)]]

   [:div.col3
    col3]

   [:div.route
    route]

   (when table
     [:div.table
      ; TODO
      (str table)])])

(defn- flight-strip [{:keys [callsign config arrival?] :as strip}]
  (if arrival?
    [flight-strip-form
     {:callsign callsign
      :config config
      :squawk-column [nil
                      (:arrival-fix strip)]
      :route [:<>
              [:div.route-body "TODO assigned altitudes"]
              [:div.destination (:destination strip)]]}]

    ; Departure:
    (let [cruise-flight-level 350] ; TODO compute
      [flight-strip-form
       {:callsign callsign
        :config config
        :squawk-column [nil
                        cruise-flight-level]
        :col3 [:<>
               [:div.origin (:origin strip)]
               [:div.departure-fix (:departure-fix strip)]]
        :route (get-in strip [:route :route])}])))

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
   :padding (px 4)
   :user-select :none}
  [:.strips-group-title {:background-color :*accent*
                         :border [[(px 2) :outset :*map-text*]]
                         :border-radius (px 2)
                         :font-family :sans-serif
                         :margin 0
                         :padding (px 12)
                         :text-align :center
                         :text-transform :uppercase}]
  [:.strips-group {:padding 0
                   :margin 0
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
