(ns atc.engine.queues
  (:require
   [atc.engine.model :refer [spawn-aircraft]]
   [atc.game.traffic.model :refer [next-arrival next-departure
                                   spawn-initial-arrivals]]))

(defn- create-traffic-queue [generate-fn]
  (fn traffic-queue [{engine :engine
                      {:keys [next-time-s]} :state}]
    (loop [engine engine
           next-time-s next-time-s]
      (if (> next-time-s
             (:elapsed-s engine))
        ; Nothing more to do!
        {:engine engine
         :state {:next-time-s next-time-s}}

        ; Spawn another craft
        (let [{:keys [aircraft delay-to-next-s]}
              (generate-fn (:traffic engine) engine)]
          (recur (spawn-aircraft engine aircraft)
                 (+ next-time-s delay-to-next-s)))))))

(def ^:private base-arrival-queue
  (create-traffic-queue next-arrival))

(def ^:private queues
  {:arrival (fn arrival-queue [{engine :engine
                                {:keys [next-time-s]} :state
                                :as param}]
              (if (nil? next-time-s)
                ; Spawn initial set of arrivals
                (let [{:keys [engine delay-to-next-s]} (spawn-initial-arrivals
                                                         (:traffic engine)
                                                         engine)]
                  {:engine engine
                   :state {:next-time-s delay-to-next-s}})

                ; Otherwise, just queue as normal
                (base-arrival-queue param)))

   :departure (create-traffic-queue next-departure)})

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
