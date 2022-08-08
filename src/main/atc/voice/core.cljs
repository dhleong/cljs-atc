(ns atc.voice.core
  (:require
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [atc.engine.core :refer [engine-grammar]]
   [atc.voice.mic :as mic]
   [atc.voice.recognizer :as recognizer]
   [promesa.core :as p]))

(defn pause! [client]
  (when-let [inst (::mic @client)]
    ; FIXME: If we have issued some partial results and are waiting on the
    ; final result, we actually MUST NOT pause until that occurs, or we will NEVER
    ; get the result! Perhaps if there were a way we could signal to the recognizer
    ; to *not stop until* we signal it....
    (if (nil? (::last-partial @client))
      (mic/pause! inst)

      (let [wait-start (js/Date.now)]
        ; NOTE: this is fairly impure but... fine for now:
        (>evt [:voice/busy true])

        (println "Waiting for processing to finish...")
        (add-watch client :pause-after-result
                  (fn [_ _ _ new-state]
                    (when (nil? (::last-partial new-state))
                      (remove-watch client :pause-after-result)
                      (>evt [:voice/busy false])
                      (println "Processing done! Waited " (- (js/Date.now) wait-start) "ms")
                      (pause! client))))))))

(defn resume! [client]
  (when-let [inst (::mic @client)]
    (mic/resume! inst)))

(defn stop! [client]
  (when-let [inst (::mic @client)]
    (mic/stop! inst)))

(defn create [{:keys [on-partial-result on-result on-state engine]}]
  (let [emit-state! (comp
                      #(p/delay 0) ; Let the UI update, if desired
                      (or on-state identity))

        _ (emit-state! :initializing) ; Do this before creating the recognizer

        recognizer-inst (if engine
                          (recognizer/create {:grammar (engine-grammar engine)})
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
