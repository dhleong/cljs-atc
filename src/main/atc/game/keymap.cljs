(ns atc.game.keymap
  (:require
   [goog.events.KeyCodes :as KeyCodes]))

(def keypress-rules
  {:event-keys [[[:game/toggle-paused]
                 [{:keyCode KeyCodes/ESC}]
                 [{:keyCode KeyCodes/QUESTION_MARK}]]]})

(def keydown-rules
  {:event-keys [[[:voice/set-paused false]
                 [{:keyCode KeyCodes/SPACE}]]]
   :prevent-default-keys [{:keyCode KeyCodes/SPACE}]})

(def keyup-rules
  {:event-keys [[[:voice/set-paused true]
                 [{:keyCode KeyCodes/SPACE}]]

                ; NOTE: Would like to use keypress for this, but browsers
                ; send the keyCode for lower-case-P (IE, holding shift+p) so
                ; there's no way to handle lower-case-p except via keyup...
                [[:game/toggle-paused]
                 [{:keyCode KeyCodes/P}]]]})
