(ns atc.cofx
  (:require
   [re-frame.core :refer [reg-cofx]]))

(reg-cofx
  ::now
  (fn [coeffects _]
    (assoc coeffects :now (js/Date.now))))
