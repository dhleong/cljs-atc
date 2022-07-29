(ns atc.voice.core
  (:require
   [atc.voice.mic :as mic]
   [atc.voice.recognizer :as recognizer]
   [promesa.core :as p]))

(defn create []
  (let [client (atom {::recognizer (recognizer/create)})]

    (p/do!
      (recognizer/await! (::recognizer @client))
      (mic/await! (::mic (swap! client assoc ::mic (mic/create))))

      ; Pipe the mic into the recognizer
      (let [recognizer-stream (recognizer/stream (::recognizer @client))]
        (mic/pipe-into! (::mic @client) recognizer-stream))

      ; TODO pull this out
      (doto (:atc.voice.recognizer/recognizer @(::recognizer @client))
        (.on "result" (fn [message] (println "RESULT" message)))
        (.on "partialresult" (fn [message] (println "PARTIAL" message))))

      (println "Ready!"))

    client))

#_:clj-kondo/ignore
(comment
  (def client (create)))
