(ns atc.voice.mic
  (:require
   [applied-science.js-interop :as j]
   ["microphone-stream" :default MicrophoneStream]
   [promesa.core :as p]))

(defn await! [client]
  (::ready-promise @client))

(defn pipe-into! [client destination-stream]
  (if-let [^js source (::stream @client)]
    (.pipe source destination-stream)

    (throw (ex-info "mic client not yet initialized" {:state @client}))))

(defn pause! [client]
  (swap! client (fn [state]
                  (if (:recording? state)
                    (do (.pauseRecording ^MicrophoneStream (::stream state))
                        (println "Paused recording")
                        (assoc state :recording? false))
                    state))))

(defn resume! [client]
  (swap! client (fn [state]
                  (if-not (:recording? state)
                    (do (.playRecording ^MicrophoneStream (::stream state))
                        (println "Resumed recording")
                        (assoc state :recording? true))
                    state))))

(defn- terminate! [state]
  (p/do!
    ; NOTE: It's not entirely clear why, but we need to *manually* suspend the
    ; AudioContext and stop the mic track...
   (j/call-in (::stream state) [:context :suspend])
   (-> (::media state) (.getTracks) (aget 0) (.stop))

    ; ... then *wait a frame*...
   (p/delay 0)

    ; ... before proceeding with the normal MicrophoneStream cleanup, in order to
    ; avoid Safari continuing to badge our tab as "producing audio." The "recording audio"
    ; indicator would be gone in this situation, at least.... But with this hack, at last
    ; no indicator remains!
   (.stop (::stream state))

   (println "Stopped recording")))

(defn stop! [client]
  (swap! client (fn [state]
                  (if-not (:stopped? state)
                    (do
                      (terminate! state)
                      (assoc state :recording? false :stopped? true))
                    state))))

(defn create []
  (let [client (atom {::stream (MicrophoneStream. #js {:bufferSize 1024
                                                       :objectMode true})})]
    (swap!
     client
     assoc
     ::ready-promise
     (p/let [media (j/call-in js/navigator [:mediaDevices :getUserMedia]
                              #js {:video false
                                   :audio #js {:echoCancellation true
                                               :noiseSuppression true}})
             {::keys [^MicrophoneStream stream]} (swap! client assoc ::media media :recording? true)]
       (println "Loaded media! " media)
       (.setStream stream media)))

    client))

#_:clj-kondo/ignore
(comment
  (def mic (create))

  (stop! mic)

  (pause! mic)
  (resume! mic))
