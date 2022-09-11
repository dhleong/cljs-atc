(ns atc.pronunciation-test
  (:require
   [atc.pronunciation :refer [make-pronounceable missing-words]]
   [clojure.test :refer [deftest is testing]]))

(deftest missing-words-test
  (testing "No false positives"
    (is (nil? (missing-words "sea isle")))
    (is (nil? (missing-words "rober")))))

(deftest make-pronounceable-test
  (testing "split words"
    (is (= "robbins fill"
           (make-pronounceable
             "robbinsville"))))

  (testing "don't split words too aggressively"
    ; TODO
    #_(is (= "parch"
           (make-pronounceable
             "parch")))))
