(ns atc.subs-util
  (:require
   [atc.data.core :refer [local-xy]]))

(defn navaids-by-id [airport]
  (some->>
    airport
    :navaids
    (reduce
      (fn [m {:keys [position] :as navaid}]
        (assoc m (:id navaid)
               (merge navaid (local-xy position airport))))
      {})))
