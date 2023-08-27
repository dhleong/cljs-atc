(ns atc.util.coll)

(defn- create-x-by-with-size [cmp-fn]
  (fn compute-x-by [pred coll]
    (loop [min-value nil
           min-size ::not-computed
           coll coll]
      (if-not (seq coll)
        ; Done!
        {:value min-value
         :size min-size}

        (let [v (first coll)
              size (pred v)
              smaller? (or (= ::not-computed min-size)
                           (cmp-fn size min-size))]
          (if smaller?
            (recur v size (next coll))
            (recur min-value min-size (next coll))))))))

(def ^{:doc "Pick the 'smallest' value of coll, comparing the value computed by
             the predicate `pred`."}
  min-by
  (comp :value (create-x-by-with-size <)))

(def ^{:doc "Pick the 'largest' value of coll, comparing the value computed by
             the predicate `pred`.
             Returns a map of {:value, :size}"}
  max-by-with-size
  (create-x-by-with-size >))
