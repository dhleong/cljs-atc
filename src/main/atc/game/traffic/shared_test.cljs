(ns atc.game.traffic.shared-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.data.core :refer [local-xy]]
   [atc.engine.model :refer [lateral-distance-to-squared]]
   [atc.game.traffic.shared :as shared :refer [distribute-crafts-along-route
                                               partial-arrival-route
                                               position-and-format-initial-arrivals
                                               position-arriving-aircraft]]
   [atc.subs :refer [navaids-by-id]]
   [atc.util.testing :refer [roughly=]]
   [cljs.test :refer-macros [deftest is testing]]))

(defn- create-engine []
  {:airport kjfk/airport
   :game/navaids-by-id (navaids-by-id kjfk/airport)})

(deftest partial-arrival-route-test
  (testing "Extract a partial arrival route"
    (is (= ["ENE" "PARCH" "CCC" "ROBER"]
           (partial-arrival-route
             (create-engine)
             {:route (get-in kjfk/airport [:arrival-routes "KBTV" :route])})))

    ; Handle navigating direct to a fix
    (is (= ["CAMRN"]
           (partial-arrival-route
             (create-engine)
             {:route (get-in kjfk/airport [:arrival-routes "KBWI" :route])})))))

(deftest space-crafts-along-route-test
  (testing "Position crafts along the route"
    (let [engine {:airport kjfk/airport
                  :game/navaids-by-id (navaids-by-id kjfk/airport)}
          distributed (distribute-crafts-along-route
                        engine
                        ["PARCH" "CCC" "ROBER"]
                        [{:callsign "DAL22"}
                         {:callsign "DAL23"}])
          [craft1 craft2] distributed]
      (is (roughly=
            (local-xy
              (get-in engine [:game/navaids-by-id "ROBER" :position])
              kjfk/airport)
            (dissoc (:position craft1) :z)))

      (let [distance (lateral-distance-to-squared
                       (:position craft1)
                       (:position craft2))
            delta 0.1]
        (is (<= (- @#'shared/lateral-spacing-m-squared delta)
                distance
                (+ @#'shared/lateral-spacing-m-squared delta))))))

  (testing "Gracefully handle single-fix routes"
    (let [engine {:airport kjfk/airport
                  :game/navaids-by-id (navaids-by-id kjfk/airport)}
          distributed (distribute-crafts-along-route
                        engine
                        ["CAMRN"]
                        [{:callsign "DAL22"}
                         {:callsign "DAL23"}])
          [craft1 _] distributed]
      (is (= (local-xy
               (get-in engine [:game/navaids-by-id "CAMRN" :position])
               kjfk/airport)
             (dissoc (:position craft1) :z))))))

(deftest position-and-format-initial-arrivals-test
  (testing "Prevent overlapping planes"
    ; NOTE: the LENDY8 and IGN1 arrivals both land on LENDY In an ideal
    ; world we can just shift these apart, but distinct-ifying is simpler
    (is (= 1
           (->> (position-and-format-initial-arrivals
                  (create-engine)
                  [(get-in kjfk/airport [:arrival-routes "KMKE"])
                   (get-in kjfk/airport [:arrival-routes "KSBP"])])
                count)))))

(deftest position-arriving-aircraft-test
  (testing "The first craft should be on its last navaid"
    ; NOTE: KBTV uses the ROBER2 arrival
    (let [craft (position-arriving-aircraft
                  (create-engine)
                  (get-in kjfk/airport [:arrival-routes "KBTV"]))]
      (is (roughly=
           (local-xy
             (get-in (create-engine) [:game/navaids-by-id "ROBER" :position])
             kjfk/airport)
           (dissoc (:position craft) :z)))))

  (testing "The second craft on a route should be laterally-spaced"
    (let [engine (create-engine)
          craft1 (position-arriving-aircraft
                   engine
                   (merge (get-in kjfk/airport [:arrival-routes "KBTV"])
                          {:callsign "DAL1"
                           :destination "KJFK"}))
          engine (assoc-in engine [:aircraft (:callsign craft1)] craft1)

          craft2 (position-arriving-aircraft
                   engine
                   (merge (get-in kjfk/airport [:arrival-routes "KBTV"])
                          {:callsign "DAL2"
                           :destination "KJFK"}))
          distance (lateral-distance-to-squared
                     (:position craft1)
                     (:position craft2))
          delta 0.1]
      (is (<= (- @#'shared/lateral-spacing-m-squared delta)
              distance
              (+ @#'shared/lateral-spacing-m-squared delta))))))
