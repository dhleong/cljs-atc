(ns atc.voice.core
  (:require
   [applied-science.js-interop :as j]
   [atc.voice.mic :as mic]
   [atc.voice.recognizer :as recognizer]
   [promesa.core :as p]))

(defn stop! [client]
  (when-let [inst (::mic @client)]
    (mic/stop! inst)))

(defn create [{:keys [on-partial-result on-result]}]
  (let [recognizer-inst (recognizer/create)
        client (atom {::recognizer recognizer-inst})]

    (p/do!
      (recognizer/await! recognizer-inst)
      (mic/await! (::mic (swap! client assoc ::mic (mic/create))))

      ; Pipe the mic into the recognizer
      (let [recognizer-stream (recognizer/stream recognizer-inst)]
        (mic/pipe-into! (::mic @client) recognizer-stream))

      ; Attach listeners
      (doto recognizer-inst
        (recognizer/on-partial-result! #(let [last-partial (::last-partial @client)
                                              next-partial (j/get-in % [:result :partial])]
                                          (when-not (= last-partial next-partial)
                                            (swap! client assoc ::last-partial next-partial)
                                            (when on-partial-result
                                              (on-partial-result next-partial)))))
        (recognizer/on-result! (fn [result]
                                 (swap! client dissoc ::last-partial)
                                 (when on-partial-result
                                   (on-partial-result nil))
                                 (when on-result
                                   (on-result result)))))

      (println "voice: Ready!"))

    client))

#_:clj-kondo/ignore
(comment
  (def client (create {:on-result #(println "RESULT" %)})))
