(ns atc.radio-test
  (:require
   [atc.radio :refer [->readable ->speakable format-airline-radio]]
   [cljs.test :refer-macros [deftest is testing]]))

(deftest speakable-test
  (testing "Convert speakable vector objects"
    (is (= "cleared approach runway 1 3 left"
           (->speakable [["cleared approach runway"
                          [:runway "13L"]]])))
    (is (= "we have alpha"
           (->speakable [["we have" [:letter "A"]]]))))

  (testing "Preserve maps"
    (is (= "2 4 0 delta 22"
           (->speakable [[[:heading 240]
                          {:radio-name "delta 22"}]])))))

(deftest readable-test
  (testing "Convert vector objects cleanly to a readable representation"
    (is (= "DAL22 cleared approach runway 13L"
           (->readable [[{:id "DAL22"}
                         "cleared approach runway"
                         [:runway "13L"]]])))))

(deftest format-airline-radio-test
  (testing "Support four-digit flight-numbers"
    (is (= {:callsign "DAL4297"
            :radio-name "delta 42 97"}
           (format-airline-radio "DAL" 4297))))
  (testing "Support three-digit flight-numbers"
    (is (= {:callsign "DAL942"
            :radio-name "delta niner 42"}
           (format-airline-radio "DAL" 942))))
  (testing "Support two-digit flight-numbers"
    (is (= {:callsign "DAL22"
            :radio-name "delta 22"}
           (format-airline-radio "DAL" 22)))))
