(ns atc.voice.recognizer
  (:require
   ["vosk-browser" :as Vosk]
   [applied-science.js-interop :as j]
   [atc.voice.const :as const]
   [promesa.core :as p]))

(defonce ^:private shared-model
  (delay
    (p/let [start (js/Date.now)
            model (Vosk/createModel "voice-model.tar.gz")]
      (println "Loaded model in " (- (js/Date.now) start) "ms")
      model)))

; (defn preload! []
;   @shared-model)

(defn create
  ([] (create nil))
  ([{:keys [sample-rate] :or {sample-rate const/default-sample-rate}}]
   (p/let [start (js/Date.now)
           model @shared-model
           KaldiRecognizer (j/get model :KaldiRecognizer)
           recognizer (new KaldiRecognizer sample-rate)]
     (println "Prepared recognizer in " (- (js/Date.now) start) "ms")
     recognizer)))

(comment

  (.then (create) #(println "SUCCESS " %) #(println "ERROR: " %)))
