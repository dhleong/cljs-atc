(ns atc.pronunciation-test
  (:require
   [atc.pronunciation :refer [missing-words]]
   [clojure.test :refer [deftest is testing]]))

(deftest missing-words-test
  (testing "No false positives"
    (is (nil? (missing-words "sea isle")))))
