(ns atc.views.game-setup
  (:require
   ["tone" :as Tone]
   [archetype.util :refer [<sub >evt]]
   [atc.config :as config]
   [atc.data.airports :refer [list-airports]]
   [atc.styles :refer [full-screen]]
   [garden.units :refer [px]]
   [promesa.core :as p]
   [reagent.core :as r]
   [santiago.form :refer [form]]
   [santiago.input :refer [input]]
   [santiago.select :refer [select]]
   [spade.core :refer [defattrs]]))

(defattrs last-game-info-attrs []
  {:margin-right (px 16)
   :background :*background-secondary*
   :border-radius (px 4)
   :padding (px 32)}
  [:.title {:margin 0}]
  [:.stats {:text-align :left
            :margin [[(px 16) 0]]}])

(defn- last-game-info []
  (when-let [{:keys [airport counts elapsed-time]} (<sub [:last-game/summary])]
    [:div (last-game-info-attrs)
     [:h3.title "Last game"]

     [:table.stats
      [:tbody
       [:tr
        [:th "Airport"]
        [:td (:id airport)]]
       [:tr
        [:th "Elapsed Time"]
        [:td elapsed-time]]
       [:tr
        [:th "Aircraft Deparated"]
        [:td (:aircraft-departed counts 0)]]]]

     [:button {:on-click #(>evt [:game/resume-last])}
      "Resume"]]))

(defattrs setup-container-attrs []
  {:composes (full-screen)
   :background :*background*
   :color :*text*
   :flex 1}
  [:.title {:margin 0}]
  [:.content {:align-items :center
              :background :*background-secondary*
              :border-radius (px 4)
              :display :flex
              :flex-direction :column
              :padding (px 32)
              :width (px 400)}]
  [:.status {:visibility :hidden
             :padding (px 16)}
   [:&.loading {:visibility :visible}]]

  [:.form {:display :flex
           :align-items :center
           :flex-direction :column
           :gap (px 8)}
   [:&.loading {:opacity 0.5}]]

  [:.form-section {:display :flex
                   :flex-direction :column
                   :align-items :center}]

  [:.labeled {:display :flex
              :font-size :105%
              :gap (px 4)
              :user-select :none
              :align-items :center}]
  [:.explanation {:text-align :center
                  :font-size :95%}]
  [:.spacer {:height (px 8)}])

(defn- start-game! [loading?-ref ^js e
                    {:keys [airport-id voice-input? enhanced-audio?
                            arrivals? departures?]}]
  (.preventDefault e)

  (when enhanced-audio?
    ; NOTE: We need to initialize the audio context from a user interaction
    ; TODO: Hide this in a cljs module pls. Also, start warming up the module!
    (Tone/start))

  (p/do
    (reset! loading?-ref true)
    (p/delay 10) ; Leave time to show loading state
    (>evt [:game/init {:airport-id airport-id
                       :arrivals? arrivals?
                       :departures? departures?
                       :voice-input? voice-input?}])))

(defn- labeled-input [{:keys [type disabled label key on-click]}]
  [:div.labeled
   [:label {:for key} label]
   [input {:type type
           :disabled disabled
           :on-click on-click
           :key key
           :id key}]])

(defn view []
  (r/with-let [form-value (r/atom (or (<sub [:game-options])
                                      config/default-game-options))
               loading? (r/atom false)
               on-start-game (partial start-game! loading?)]
    [:div (setup-container-attrs)
     [last-game-info]

     [:div.content
      [:h1.title "cljs-atc"]

      [:div.status {:class (when @loading? :loading)}
       "Reticulating splines..."]

      [form {:class [:form (when @loading? :loading)]
             :model form-value
             :on-submit on-start-game}

       [:div.labeled
        [:label {:for ::airport-id}
         "Airport"]
        [select {:id ::airport-id
                 :disabled @loading?
                 :key :airport-id}
         (for [{:keys [key label]} (list-airports)]
           ^{:key key}
           [:option {:key key} label])]]

       [:div.form-section
        [:h2 "Traffic"]
        [labeled-input {:type :checkbox
                        :disabled (or @loading?
                                      (not (:departures? @form-value)))
                        :label "Enable Arrivals"
                        :key :arrivals?}]
        [labeled-input {:type :checkbox
                        :disabled (or @loading?
                                      (not (:arrivals? @form-value)))
                        :label "Enable Departures"
                        :key :departures?}]]

       [:div.form-section
        [:h2 "Gameplay"]
        [labeled-input {:type :checkbox
                        :disabled @loading?
                        :label "Use voice input"
                        :key :voice-input?}]
        [:div.explanation {:id ::voice-explanation}
         "If enabled, you will be prompted to allow microphone input once the game is loaded. You can then hold the spacebar to activate the mic and talk to pilots on your frequency!"]

        [:div.spacer]

        [labeled-input {:type :checkbox
                        :disabled @loading?
                        :label "Use enhanced audio"
                        :key :enhanced-audio?}]
        [:div.explanation {:id ::enhanced-audio-explanation}
         "Enhance! This will use a local AI model to generate more realistic-sounding radio audio, which may be taxing for your machine."]]

       [:div.spacer]

       [:button {:disabled @loading?
                 :type :submit}
        "Start a new game"]]]]))
