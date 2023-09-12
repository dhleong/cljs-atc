(ns atc.engine.aircraft-test
  (:require
   [atc.data.aircraft-configs :as configs]
   [atc.engine.aircraft :refer [choose-cruise-altitude-fl departing-bearing]]
   [atc.util.testing :refer [create-engine roughly=]]
   [clojure.test :refer [deftest is testing]]))

(deftest departing-bearing-test
  (testing "Compute a rough departing bearing based on known navaids"
    (let [engine (create-engine)
          craft {:departure-fix "RBV"}]
      (is (roughly= 244 (departing-bearing engine craft)
                    :delta 0.5)))))

(deftest choose-curise-altitude-fl-test
  (testing "Follow SWEVEN rules"
    (let [engine (create-engine)
          craft {:departure-fix "RBV"
                 :config configs/common-jet}]
      (is (= 360 (choose-cruise-altitude-fl
                   engine craft))))))
