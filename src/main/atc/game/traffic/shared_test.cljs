(ns atc.game.traffic.shared-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.engine.model :refer [distance-to-squared]]
   [atc.game.traffic.shared :as shared :refer [distribute-crafts-along-route
                                               partial-arrival-route]]
   [atc.subs :refer [navaids-by-id]]
   [cljs.test :refer-macros [deftest is testing]]))

(deftest partial-arrival-route-test
  (testing "Extract a partial arrival route"
    (is (= ["PARCH" "CCC" "ROBER"]
           (partial-arrival-route
             kjfk/airport
             {:route (get-in kjfk/airport [:arrival-routes "KBTV" :route])})))))

(deftest space-crafts-along-route-test
  (testing "Position crafts along the route"
    (let [engine {:game/navaids-by-id (navaids-by-id kjfk/airport)}
          distributed (distribute-crafts-along-route
                        engine
                        ["PARCH" "CCC" "ROBER"]
                        [{:callsign "DAL22"}
                         {:callsign "DAL23"}])
          [craft1 craft2] distributed]
      (is (= (get-in engine [:game/navaids-by-id "ROBER" :position])
             (:position craft1)))

      (is (= #'shared/lateral-spacing-m-squared
             (distance-to-squared
               (:position craft1)
               (:position craft2)))))))
