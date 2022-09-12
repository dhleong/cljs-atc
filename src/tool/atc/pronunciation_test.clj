(ns atc.pronunciation-test
  (:require
   [atc.pronunciation :refer [make-pronounceable missing-words]]
   [clojure.test :refer [deftest is testing]]))

(deftest missing-words-test
  (testing "No false positives"
    (is (nil? (missing-words "sea isle")))
    (is (nil? (missing-words "rober")))))

(deftest make-pronounceable-test
  (testing "Split words"
    (is (= "robbins fill"
           (make-pronounceable
             "robbinsville"))))

  (testing "Don't split words *too* aggressively"
    (is (= "parch"
           (make-pronounceable
             "parch"))))

  (testing "Add trailing `e`"
    (is (= "deedee"
           (make-pronounceable
             "deede"))))

  (testing "Strip trailing `e`"
    (is (= "coat"
           (make-pronounceable
             "coate")))
    (is (= "door"
           (make-pronounceable
             "doore"))))

  (testing "Dedup letters"
    (is (= "kars"
           (make-pronounceable
             "karrs")))
    (is (= "hays"
           (make-pronounceable
             "haays"))))

  (testing "Swap letters"
    (is (= "gayle"
           (make-pronounceable
             "gayel")))
    (is (= "candor"
           (make-pronounceable
             "candr")))
    (is (= "gam bee"
           (make-pronounceable
             "gamby")))
    (is (= "len dee"
           (make-pronounceable
             "lendy")))

    (is (= "a cove"
           (make-pronounceable
             "acove")))

    (is (= "wave ee"
           (make-pronounceable
             "wavey")))))
