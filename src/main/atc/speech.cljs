(ns atc.speech
  (:require
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [promesa.core :as p]
   [clojure.string :as str]))

(defn- load-voices []
  (->> (js/window.speechSynthesis.getVoices)
       (map (fn [instance]
              {:name (j/get instance :name)
               :lang (j/get instance :lang)
               ::instance instance}))
       (filterv #(str/starts-with? (:lang %) "en"))))

(def ^:private state (atom {:active nil}))

; NOTE: We shouldn't need this ultimately, but it'll be convenient for now...
(def ^:private shared-voices
  (delay
    (load-voices)))

(defn init []
  (if-not js/window.speechSynthesis
    (>evt [:speech/unavailable])
    (>evt [:speech/on-voices-changed (load-voices)])))

(defn pick-random-voice []
  (rand-nth @shared-voices))

(defn say! [{:keys [message pitch rate voice]
             :or {rate 1 pitch 1}}]
  (p/create
    (fn [p-resolve p-reject]
      (let [clear-active #(swap! state dissoc :active)
            utt (doto (js/SpeechSynthesisUtterance. message)
                  (j/assoc! :rate rate)
                  (j/assoc! :pitch pitch)
                  (j/assoc! :voice (::instance voice voice))

                  (.addEventListener "start" (fn []
                                               (let [{:keys [timeout]} @state]
                                                 (js/clearTimeout timeout))))
                  (.addEventListener "end" (comp p-resolve clear-active))
                  (.addEventListener "error" (comp p-reject clear-active)))]
        ; NOTE: We stash the utterance in some state to avoid it getting GC'd before
        ; it completes (which could result in listeners not firing)
        ; NOTE: Also, occasionally it seems like the speech never starts.
        ; So, we set a timeout here and clear it on `start`---that's the
        ; common (success) case; if the timeout actually fires, we'll reject
        ; the promise to allow the voice state to get cleared out.
        (reset! state {:active utt
                       :timeout (js/setTimeout
                                  (partial
                                    (p-reject
                                      (ex-info "Speech synthesis timeout"
                                               {:message message})))
                                  750)})
        (js/window.speechSynthesis.speak utt)))))
