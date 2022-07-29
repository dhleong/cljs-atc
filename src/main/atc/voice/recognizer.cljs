(ns atc.voice.recognizer
  (:require
   [applied-science.js-interop :as j]
   ["vosk-browser" :as Vosk]
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
  ([{:keys [sample-rate] :or {sample-rate 48000}}]
   (p/let [start (js/Date.now)
           model @shared-model
           KaldiRecognizer (j/get model :KaldiRecognizer)
           recognizer (new KaldiRecognizer sample-rate)]
     (println "Prepared recognizer in " (- (js/Date.now) start) "ms")
     recognizer)))

(comment

  (.then (create) #(println "SUCCESS " %) #(println "ERROR: " %)))
