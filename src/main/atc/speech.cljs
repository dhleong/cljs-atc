(ns atc.speech
  (:require
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [clojure.string :as str]
   [promesa.core :as p]
   [atc.util.lazy :as lazy]))

(defonce ^:private mode (atom :builtin))

; NOTE: We explicitly do NOT want to require these namespaces,
; since they should be code-split
(def ^:private enhanced-init
  (lazy/function
   (lazy/dynamic-import 'atc.speech.enhanced/init)))

(def ^:private enhanced-speak
  (lazy/function
   (lazy/dynamic-import 'atc.speech.enhanced/speak)))

(def ^:private enhanced-pick-random-voice
  ; NOTE: Unlike above, we want to be able to call the imported
  ; function synchronously by deref'ing the ILoadable, so we don't
  ; wrap in lazy/function
  (lazy/dynamic-import 'atc.speech.enhanced/pick-random-voice))

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

(def ^:private shared-enhanced-prepare-promise
  (delay
    (println "preparing enhanced audio...")
    (-> (p/do
          (enhanced-init)

          ; Initialize enhanced-pick-random-voice; unpack returns a promise,
          ; and then we can just deref it synchronously once this promise
          ; has resolved
          (lazy/unpack enhanced-pick-random-voice)

          (println "Enhanced audio ready!")
          true)
        (p/catch (fn [e]
                   (reset! mode :builtin-only)
                   (js/console.warn "Failed to initialize enhanced audio..." e)
                   false)))))

(defn init []
  (if-not js/window.speechSynthesis
    (>evt [:speech/unavailable])
    (>evt [:speech/on-voices-changed (load-voices)])))

(defn prepare! [{:keys [enhanced?]}]
  (swap! mode (fn [existing]
                (cond
                  ; If :builtin-only, we failed to initialize enhanced
                  (= existing :builtin-only) :builtin-only
                  enhanced? :enhanced
                  :else :builtin)))

  (when enhanced?
    @shared-enhanced-prepare-promise))

(defn pick-random-voice []
  (case @mode
    (:builtin :builtin-only) (rand-nth @shared-voices)
    :enhanced (@enhanced-pick-random-voice)))

(defn- say-synthesis! [{:keys [message pitch rate voice]
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

(defn say! [{:keys [enhanced?] :as task}]
  ; NOTE: We don't normally set enhanced? one way or another... but could be nice?
  (if (or (and (not (false? enhanced?))
               (= @mode :enhanced))
          (true? enhanced?))
    (-> (enhanced-speak task)
        (p/catch (fn [e]
                   (js/console.error "Failed to enhanced-speak" e)
                   (println "Task: " task)
                   ; Fall back to builtin speech, just in case
                   (reset! mode :builtin-only))))
    (say-synthesis! task)))
