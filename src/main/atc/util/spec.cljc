(ns atc.util.spec
  (:require [clojure.spec.alpha :as s]))

(defn pre-validate
  "Throw an exception if db doesn't have a valid spec."
  [spec value]
  (if (s/valid? spec value)
    true
    (do
      (tap> value)
      (let [explanation (s/explain-str spec value)]
        (println "Spec check failed: " explanation)
        (println " value: " value)
        false))))
