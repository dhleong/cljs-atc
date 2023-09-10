(ns atc.fx
  (:require
   [archetype.nav :as nav]
   [archetype.util :refer [>evt]]
   [atc.game :as game]
   [atc.speech :as speech]
   [atc.util.local-storage :as local-storage]
   [atc.voice.core :as voice]
   [atc.voice.process :refer [find-command]]
   [clojure.core.match :refer [match]]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]))

; This effect is unused... for now (and that's okay)
#_:clj-kondo/ignore
(reg-fx
  :nav/replace!
  nav/replace!)


; ======= Deferrables =====================================
; Like dispatch-later, but cancelable!

(defonce ^:private deferred-timeouts
  (atom {}))

(defn- defer-cancel [state cancel-spec]
  (match cancel-spec
    :all
    (do
      (doseq [[_ v] state]
        (js/clearTimeout v))
      {})

    {:id id}
    (do
      (when-some [old (get state id)]
        (js/clearTimeout old))
      (dissoc state id))

    :else
    (js/console.error "Unexpected defer/cancel spec: " cancel-spec)
    state))

(defn- defer [state {:keys [id ms dispatch]}]
  (defer-cancel state {:id id})

  (assoc state id (js/setTimeout
                    #(>evt dispatch)
                    ms)))

(reg-fx
  :defer
  (fn [deferrable]
    (swap! deferred-timeouts defer deferrable)))

(reg-fx
  :defer/cancel
  (fn [cancel-spec]
    (swap! deferred-timeouts defer-cancel cancel-spec)))

; ======= Persistence =====================================

(reg-fx
  :local-storage/save
  (fn [[key data]]
    (local-storage/save key data)))


; ======= Game setup ======================================

(reg-fx
  :game/init-async
  game/init-async)

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
  (fn [{:keys [machine input]}]
    (println "Processing: " input)
    (when-let [cmd (find-command machine input)]
      (>evt [:game/command cmd]))))


; ======= Speech Output ===================================

(reg-fx
  :speech/say
  (fn [{:keys [message on-complete] :as opts}]
    (-> (speech/say! opts)
        (p/catch (fn [e] (println "[:speech/say] Failed to speak `" message "`: " e)
                   (on-complete)))
        (p/then (fn [_] (println "[:speech/say] Spoke `" message "`")
                  (on-complete))))))

(comment
  (>evt [:voice/set-paused false])
  (>evt [:voice/set-paused true])
  (>evt [:voice/start!])
  (>evt [:voice/start! {:use-grammar? true}])
  (>evt [:voice/stop!]))
