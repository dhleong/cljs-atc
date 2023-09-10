(ns atc.game.traffic.random
  (:require
   [atc.config :as config]
   [atc.data.aircraft-configs :as configs]
   [atc.data.airlines :refer [all-airlines]]
   [atc.engine.model :refer [spawn-aircraft]]
   [atc.game.traffic.model :refer [ITraffic next-arrival]]
   [atc.game.traffic.shared :refer [position-arriving-aircraft]]
   [atc.util.seedable :refer [next-boolean next-int pick-random]]))

(defrecord RandomTraffic [random]
  ITraffic
  (spawn-initial-arrivals [this engine]
    (let [[engine delay-to-next-s]
          (reduce
            (fn [[engine'] _]
              (let [{:keys [aircraft delay-to-next-s]} (next-arrival this engine')]
                [(spawn-aircraft engine' aircraft) delay-to-next-s]))
            [engine nil]
            ; TODO "difficulty?"
            (range 0 config/initial-arrivals-to-spawn))]
      {:engine engine
       :delay-to-next-s delay-to-next-s}))

  (next-arrival [_ {:keys [airport] :as engine}]
    (let [origin (pick-random
                   random
                   (-> airport :arrival-routes keys))

          ; Will this aircraft get the weather before contacting approach?
          will-get-weather? (next-boolean random)

          craft {:type :airline
                 :airline (pick-random random (keys all-airlines))
                 :flight-number (next-int random 20 9999)
                 :origin origin
                 :destination (:id airport)
                 :route (-> airport (get-in [:arrival-routes origin :route]))
                 :behavior {:will-get-weather? will-get-weather?}
                 :config configs/common-jet}]
      {:aircraft (position-arriving-aircraft engine craft)

       ; TODO Maybe depend on "difficulty"?
       :delay-to-next-s 240}))

  (next-departure [_ {:keys [airport]
                      runways :game/active-runways}]
    (if-not runways
      {:delay-to-next-s 1}

      (let [destination (pick-random
                          random
                          (-> airport :departure-routes keys))]
        {:aircraft {:type :airline
                    :airline (pick-random random (keys all-airlines))
                    :flight-number (next-int random 20 9999)
                    :destination destination
                    :route (-> airport (get-in [:departure-routes destination]))

                    ; TODO Round-robin runway selection, if multiple available
                    :runway (->> runways :departures first)
                    :config configs/common-jet}

         ; TODO This should at least depend on the spawned aircraft's speed, etc. Maybe "difficulty"?
         :delay-to-next-s 240}))))
