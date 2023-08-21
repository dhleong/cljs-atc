(ns atc.game.traffic.shared-test
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.game.traffic.shared :refer [partial-arrival-route
                                    space-crafts-along-route]]
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
    (let [engine {:game/navaids-by-id (navaids-by-id kjfk/airport)}]
      (is (= (get-in engine [:game/navaids-by-id "ROBER" :position])
             (->>
               (space-crafts-along-route
                 engine
                 ["PARCH" "CCC" "ROBER"]
                 [{:callsign "DAL22"}])
               first
               :position))))))
