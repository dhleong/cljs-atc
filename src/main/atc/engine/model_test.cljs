(ns atc.engine.model-test
  (:require
   [atc.engine.model :refer [angle-down-to]]
   [atc.util.testing :refer [roughly=]]
   [clojure.test :refer [deftest is testing]]))

(deftest angle-down-to-test
  (testing "Compute angle to ground position"
    (is (= 45 (angle-down-to
                {:x 0 :y 10 :z 12}
                {:x 0 :y 0 :z 2})))
    (is (roughly= 31 (angle-down-to
                       {:x 0 :y 10 :z 8}
                       {:x 0 :y 0 :z 2})))))
