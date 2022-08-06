(ns atc.fx
  (:require
   [archetype.nav :as nav]
   [archetype.util :refer [>evt]]
   [atc.speech :as speech]
   [atc.voice.core :as voice]
   [atc.voice.process :refer [find-command]]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]))

; This effect is unused... for now (and that's okay)
#_:clj-kondo/ignore
(reg-fx
  :nav/replace!
  nav/replace!)


; ======= Voice input =====================================

(defonce voice-client (atom nil))

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
    (println "Processing: " input)
    (when-let [cmd (find-command input)]
      (>evt [:game/command cmd]))))


; ======= Speech Output ===================================

(reg-fx
  :speech/say
  (fn [{:keys [message on-complete] :as opts}]
    (-> (speech/say! opts)
        (p/catch (fn [e] (println "Failed to speak `" message "`: " e)))
        (p/finally on-complete))))

(comment
  (>evt [:voice/set-paused false])
  (>evt [:voice/set-paused true])
  (>evt [:voice/start!])
  (>evt [:voice/start! {:use-grammar? true}])
  (>evt [:voice/stop!]))
