(ns atc.radio-test
  (:require
   [atc.radio :refer [->readable ->speakable]]
   [cljs.test :refer-macros [deftest is testing]]))

(deftest speakable-test
  (testing "Convert speakable vector objects"
    (is (= "cleared approach runway 1 3 left"
           (->speakable [["cleared approach runway"
                          [:runway "13L"]]]))))

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
