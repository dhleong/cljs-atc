(ns atc.weather.metar-test
  (:require
   [atc.weather.metar :as metar]
   [clojure.test :refer [deftest is testing]]))

(deftest parse-text-test
  (testing "Simple wind"
    (is (= {:altimeter "30.26"
            :date-time "012151Z"
            :dewpoint-c -13
            :temperature-c 21
            :visibility-sm 10
            :wind-heading 150
            :wind-kts 8}

           (metar/parse-text
             "KJFK 012151Z 15008KT 10SM FEW250 21/M13 A3026 RMK AO2 SLP248 T02060128"))))

  (testing "Gusting wind"
    (is (= {:wind-heading 150
            :wind-kts 8}

           (->
             "KJFK 012151Z 15008G20KT 10SM FEW250 21/M13 A3026 RMK AO2 SLP248 T02060128"
             (metar/parse-text)
             (select-keys [:wind-heading :wind-kts])))))

  (testing "Low visibility"
    (is (= {:visibility-sm (/ 1 2)}

           (->
             "KJFK 012151Z 15008G20KT 1/2SM FEW250 21/M13 A3026 RMK AO2 SLP248 T02060128"
             (metar/parse-text)
             (select-keys [:visibility-sm]))))))