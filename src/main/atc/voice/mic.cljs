(ns atc.voice.mic
  (:require
   [applied-science.js-interop :as j]
   ["microphone-stream" :default MicrophoneStream]
   [promesa.core :as p]))

(defn pause! [client]
  (swap! client (fn [state]
                  (if (:recording? state)
                    (do (.pauseRecording (::stream state))
                        (println "Paused recording")
                        (assoc state :recording? false))
                    state))))

(defn resume! [client]
  (swap! client (fn [state]
                  (if-not (:recording? state)
                    (do (.playRecording (::stream state))
                        (println "Resumed recording")
                        (assoc state :recording? true))
                    state))))

(defn stop! [client]
  (swap! client (fn [state]
                  (if-not (:stopped? state)
                    (do (.stop (::stream state))
                        (println "Stopped recording")
                        (assoc state :recording? false :stopped? true))
                    state))))

(defn create []
  (let [client (atom {::stream (MicrophoneStream.)})]
    (p/let [media (j/call-in js/navigator [:mediaDevices :getUserMedia]
                             #js {:video false
                                  :audio #js {:echoCancellation true
                                              :noiseSuppression true}})
            {::keys [stream]} (swap! client assoc ::media media :recording? true)]
      (println "Loaded media! " media)
      (.setStream stream media))
    client))

#_:clj-kondo/ignore
(comment
  (def mic (create))

  (stop! mic)

  (pause! mic)
  (resume! mic))
