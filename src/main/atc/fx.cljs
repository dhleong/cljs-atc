(ns atc.fx
  (:require [re-frame.core :refer [reg-fx]]
            [archetype.nav :as nav]
            [atc.voice.core :as voice]))

(reg-fx
  :nav/replace!
  nav/replace!)

(def voice-client (atom nil))

(reg-fx
  :voice/start!
  (fn []
    (swap! voice-client (fn [_old]
                          ; TODO stop old
                          ; (when old) 

                          (voice/create)))))
