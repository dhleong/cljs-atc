(ns atc.views.game.controls
  (:require
   [archetype.util :refer [<sub >evt]]
   ["react" :as React]
   [spade.core :refer [defattrs]]))

(defattrs voice-controls-active-attrs [recording?]
  [:.mic {:opacity (if recording? 0.8 1.0)}])

(defn- voice-controls-active [{:keys [show-disable?]}]
  (React/useEffect
    (fn []
      (>evt [:voice/enable-keypresses])
      #(>evt [:voice/disable-keypresses])))

  [:div (voice-controls-active-attrs (<sub [:voice/recording?]))
   (when show-disable?
     [:button {:on-click #(>evt [:voice/stop!])}
      "Disable Mic"])

   (when (= :ready (<sub [:voice/state]))
     [:<>
      [:button.mic {:on-mouse-down #(>evt [:voice/set-paused false])
                    :on-mouse-up #(>evt [:voice/set-paused true])}
       "MIC (hold this or space)"]
      (<sub [:voice/partial])])])

(defn- voice-controls []
  (let [voice-requested? (<sub [:voice/requested?])
        state (<sub [:voice/state])]
    (if voice-requested?
      (case state
        (nil :ready) [:f> voice-controls-active {:show-disable? false}]
        [:button {:disabled true}
         "(" (name state) ")"])

      ; Voice input not requested with setup; show controls to enable
      (case state
        nil [:button {:on-click #(>evt [:voice/start!])}
             "Enable Microphone"]
        :ready [:f> voice-controls-active]
        [:button {:disabled true}
         "(" (name state) ")"]))))

(defattrs game-controls-attrs []
  {:display :flex
   :flex-direction :row})

(defn game-controls []
  (let [time-scale (<sub [:game/time-scale])
        game-running? (some? time-scale)]
    [:div (game-controls-attrs)
     (if game-running?
       [:button {:on-click #(>evt [:game/reset])}
        "End Game"]
       [:button {:on-click #(>evt [:game/init {:airport-id :kjfk}])}
        "Init Game @ KJFK"])

     (when game-running?
       [:<>
       (if (= 0 time-scale)
         [:button {:on-click #(>evt [:game/set-time-scale 1])}
          "Resume"]
         [:button {:on-click #(>evt [:game/set-time-scale 0])}
          "Pause"])

       ; NOTE: We require a running game to initialize voice, since the voice grammar
       ; depends on the airport
       [voice-controls]])]))
