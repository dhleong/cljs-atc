(ns atc.game.traffic.shared-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.data.core :refer [local-xy]]
   [atc.engine.model :refer [lateral-distance-to-squared]]
   [atc.game.traffic.shared :as shared :refer [distribute-crafts-along-route
                                               partial-arrival-route]]
   [atc.subs :refer [navaids-by-id]]
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
      (is (= (local-xy
               (get-in engine [:game/navaids-by-id "ROBER" :position])
               kjfk/airport)
             (dissoc (:position craft1) :z)))

      (let [distance (lateral-distance-to-squared
                       (:position craft1)
                       (:position craft2))
            delta 0.1]
        (is (<= (- @#'shared/lateral-spacing-m-squared delta)
                distance
                (+ @#'shared/lateral-spacing-m-squared delta)))))))
