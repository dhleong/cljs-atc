(ns atc.fx
  (:require
   [archetype.nav :as nav]
   [archetype.util :refer [>evt]]
   [atc.voice.core :as voice]
   [atc.voice.process :refer [find-command]]
   [re-frame.core :refer [reg-fx]]))

; This effect is unused... for now (and that's okay)
#_:clj-kondo/ignore
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
  (fn [opts]
    (swap! voice-client (fn [old]
                          (when old
                            (voice/stop! old))

                          (voice/create
                            (assoc
                              opts
                              :on-partial-result #(>evt [:voice/on-partial %])
                              :on-state #(>evt [:voice/set-state %])
                              :on-result #(>evt [:voice/on-result %])))))))

(reg-fx
  :voice/set-paused
  (fn [paused?]
    (if-let [client @voice-client]
      (if paused?
        (voice/pause! client)
        (voice/resume! client))

      (println "WARNING: No voice client to handle :voice/set-paused" paused?))))

(reg-fx
  :voice/process
  (fn [input]
    (when-let [cmd (find-command input)]
      (>evt [:command/handle cmd]))))

(comment
  (>evt [:voice/start!])
  (>evt [:voice/start! {:use-grammar? true}])
  (>evt [:voice/stop!]))
