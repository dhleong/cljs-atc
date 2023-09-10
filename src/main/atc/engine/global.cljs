(ns atc.engine.global
  (:require
   [archetype.util :refer [>evt]]))

(defmulti dispatch-global-instruction (fn [_engine [instruction]] instruction))

(defmethod dispatch-global-instruction
  :update-atis
  [engine [_ letter]]
  ; TODO This should probably reward/deduct points.
  ; TODO We should check if they've already done this or not
  (if (= (get-in engine [:weather :atis])
         letter)
    (>evt [:help/reward "Thank you for handling the updated ATIS!"])
    (>evt [:help/warning (str "Information " letter " is NOT current!")]))
  engine)

(defmethod dispatch-global-instruction
  :default
  [engine [instruction]]
  (println "TODO: Unhandled global instruction: " instruction)
  engine)
