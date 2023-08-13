(ns atc.engine.queues
  (:require
   [atc.engine.model :refer [spawn-aircraft]]
   [atc.game.traffic.model :refer [next-departure]]))

(def ^:private queues
  {:departure (fn departure-queue [{engine :engine
                                    {:keys [next-time-s]} :state}]
                (loop [engine engine
                       next-time-s next-time-s]
                  (if (> next-time-s
                         (:elapsed-s engine))
                    ; Nothing more to do!
                    {:engine engine
                     :state {:next-time-s next-time-s}}

                    ; Spawn another departure
                    (let [{:keys [aircraft delay-to-next-s]}
                          (next-departure (:traffic engine) engine)]
                      (recur (spawn-aircraft engine aircraft)
                             (+ next-time-s delay-to-next-s))))))})

(defn run-queues [engine]
  (reduce-kv
    (fn [engine' queue-key queue-fn]
      (let [result (queue-fn {:engine engine'
                              :state (get-in engine' [:queues queue-key])})]
        (assoc-in (:engine result) [:queues queue-key] (:state result))))
    engine
    queues))

#_{:clj-kondo/ignore [:unresolved-namespace]}
(comment

  (vector
    (get-in @re-frame.db/app-db [:engine :elapsed-s])
    (get-in @re-frame.db/app-db [:engine :queues :departure])))
