(ns atc.engine.aircraft.instructions-test
  (:require
   [atc.data.units :refer [ft->m]]
   [atc.engine.aircraft.instructions :refer [dispatch-instruction]]
   [atc.engine.model :refer [vec3]]
   [atc.util.testing :refer [create-engine]]
   [clojure.test :refer [deftest is testing]]))

(deftest adjust-altitude-test
  (testing "Track altitude assignment when adjusted"
    (let [engine (create-engine)
          craft {:position (vec3 0 0 (ft->m 10000))}
          craft' (-> craft
                     (dispatch-instruction engine [:adjust-altitude 22000]))
          craft'' (-> craft'
                      (dispatch-instruction engine [:adjust-altitude 8000]))]
      (is (= [{:direction :climb
               :altitude-ft 22000}]
             (:altitude-assignments craft')))
      (is (= [{:direction :climb
               :altitude-ft 22000}
              {:direction :descend
               :altitude-ft 8000}]
             (:altitude-assignments craft''))))))
