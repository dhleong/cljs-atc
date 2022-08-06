(ns atc.voice.recognizer
  (:require
   ["readable-stream" :refer [Duplex]]
   ["vosk-browser" :as Vosk]
   [applied-science.js-interop :as j]
   [atc.config :as config]
   [atc.voice.const :as const]
   [atc.voice.grammar :as grammar]
   [promesa.core :as p]))

(defonce ^:private shared-model
  (delay
    (p/let [start (js/Date.now)
            model (Vosk/createModel (str config/server-root "voice-model.tar.gz"))]
      (println "Loaded model in " (- (js/Date.now) start) "ms")
      model)))

; (defn preload! []
;   @shared-model)

(defn await! [client]
  (::ready-promise @client))

(defn on-partial-result! [client callback]
  (.on (::recognizer @client) "partialresult" callback))

(defn on-result! [client callback]
  (.on (::recognizer @client) "result"
       (j/fn [ev]
         (callback (j/get-in ev [:result :text])))))

(defn stream [client]
  (Duplex.
    #js {:objectMode true
         :write
         (fn write [chnk _encoding callback]
           (let [buffer (.getChannelData chnk 0)]
             (when (> (j/get buffer :byteLength) 0)
               (when-let [^js recognizer (::recognizer @client)]
                 (.acceptWaveform recognizer chnk))))

           (callback))}))

(defn create
  ([] (create nil))
  ([{:keys [sample-rate use-grammar?] :or {sample-rate const/default-sample-rate}}]
   (let [client (atom nil)]
     (swap!
       client
       assoc
       ::ready-promise
       (p/let [start (js/Date.now)
               model @shared-model
               KaldiRecognizer (j/get model :KaldiRecognizer)
               grammar-content (when use-grammar?
                                 (println "Generating grammar...")
                                 (time (grammar/generate)))
               recognizer (if (some? grammar-content)
                            (new KaldiRecognizer sample-rate grammar-content)
                            (new KaldiRecognizer sample-rate))]
         (println "Prepared recognizer in " (- (js/Date.now) start) "ms")
         (swap! client assoc ::recognizer recognizer)))
     client)))

(comment

  (.then (create) #(println "SUCCESS " %) #(println "ERROR: " %)))
