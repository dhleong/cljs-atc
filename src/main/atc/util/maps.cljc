(ns atc.util.maps)

(defn rename-key [m k k']
  (-> m
      (dissoc k)
      (assoc k' (get m k))))
