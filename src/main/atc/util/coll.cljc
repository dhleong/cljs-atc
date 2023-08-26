(ns atc.util.coll)

(defn min-by
  "Pick the 'smallest' value of coll, comparing the value computed by the
  predicate `pred`."
  [pred coll]
  (loop [min-value nil
         min-size ::not-computed
         coll coll]
    (if-not (seq coll)
      ; Done!
      min-value

      (let [v (first coll)
            size (pred v)
            smaller? (or (= ::not-computed min-size)
                         (< size min-size))]
        (if smaller?
          (recur v size (next coll))
          (recur min-value min-size (next coll)))))))
