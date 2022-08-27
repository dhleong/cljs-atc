(ns atc.radio-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [atc.radio :refer [->speakable]]))

(deftest speakable-test
  (testing "Convert speakable vector objects"
    (is (= "cleared approach runway 1 3 left"
           (->speakable [["cleared approach runway"
                          [:runway "13L"]]]))))

  (testing "Preserve maps"
    (is (= "2 4 0 delta 22"
           (->speakable [[[:heading 240]
                          {:radio-name "delta 22"}]])))))
