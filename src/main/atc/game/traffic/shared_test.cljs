(ns atc.game.traffic.shared-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.data.core :refer [local-xy]]
   [atc.engine.model :refer [lateral-distance-to-squared]]
   [atc.game.traffic.shared :as shared :refer [partial-arrival-route
                                               position-arriving-aircraft]]
   [atc.subs :refer [navaids-by-id]]
   [atc.util.testing :refer [roughly=]]
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.math :refer [sqrt]]))

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
          distance (sqrt
                     (lateral-distance-to-squared
                       (:position craft1)
                       (:position craft2)))]
      (is (roughly=
            @#'shared/lateral-spacing-m
            distance)))))
