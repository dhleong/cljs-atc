(ns atc.voice.core
  (:require
   [applied-science.js-interop :as j]
   [atc.voice.mic :as mic]
   [atc.voice.recognizer :as recognizer]
   [promesa.core :as p]))

(defn stop! [client]
  (when-let [inst (::mic @client)]
    (mic/stop! inst)))

(defn create [{:keys [on-partial-result on-result on-state use-grammar?]}]
  (let [emit-state! (comp
                      #(p/delay 0) ; Let the UI update, if desired
                      (or on-state identity))

        _ (emit-state! :initializing) ; Do this before creating the recognizer

        recognizer-inst (if use-grammar?
                          (recognizer/create {:use-grammar? true})
                          (recognizer/create))
        client (atom {::recognizer recognizer-inst})]
    (p/do!
      (recognizer/await! recognizer-inst)

      (emit-state! :requesting-mic)
      (mic/await! (::mic (swap! client assoc ::mic (mic/create))))

      (emit-state! :opening-mic)

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

      (emit-state! :ready)
      (println "voice: Ready!"))

    client))

#_:clj-kondo/ignore
(comment
  (def client (create {:on-result #(println "RESULT" %)})))
