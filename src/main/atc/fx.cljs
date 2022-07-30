(ns atc.fx
  (:require
   [archetype.nav :as nav]
   [archetype.util :refer [>evt]]
   [atc.voice.core :as voice]
   [re-frame.core :refer [reg-fx]]))

(reg-fx
  :nav/replace!
  nav/replace!)

(def voice-client (atom nil))

(reg-fx
  :voice/stop!
  (fn []
    (swap! voice-client (fn [client]
                          (when client
                            (voice/stop! client))

                          ; Ensure we clear the instance:
                          nil))))

(reg-fx
  :voice/start!
  (fn []
    (swap! voice-client (fn [old]
                          (when old
                            (voice/stop! old))

                          (voice/create
                            {:on-partial-result #(>evt [:voice/on-partial %])
                             :on-result #(>evt [:voice/on-result %])})))))
