(ns atc.engine.aircraft.commands-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [atc.engine.aircraft.commands :refer [shorter-steer-direction]]))

(deftest shorter-steer-direction-test
  (testing "Simple checks"
    (is (= :left (shorter-steer-direction 90 0)))
    (is (= :right (shorter-steer-direction 0 90))))

  (testing "Cross-boundary checks"
    (is (= :right (shorter-steer-direction 350 90)))
    (is (= :left (shorter-steer-direction 90 350)))))

