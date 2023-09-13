(ns atc.voice.parsing.instructions.visual-approach-test
  (:require
   [atc.voice.process-test :refer [find-instructions]]
   [clojure.test :refer [deftest is testing]]))

(deftest report-field-in-sight-test
  (testing "Report field in sight"
    (is (= [[:report-field-in-sight]]
           (find-instructions
             "piper one report field in sight")))
    (is (= [[:report-field-in-sight 10]]
           (find-instructions
             "piper one ten miles from kennedy report airport in sight")))))
