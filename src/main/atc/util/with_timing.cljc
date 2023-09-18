(ns atc.util.with-timing)

(defn now []
  #? (:cljs (js/Date.now)
      :clj (System/nanoTime)))

(defn format-delta [d]
  #? (:cljs (.toFixed d 6)
      :clj (/ (double d) 1000000.0)))

(defmacro with-timing [label & expr]
  `(let [start# (now)
         ret# (do ~@expr)
         elapsed# (- (now) start#)]
     (prn (str ~(str "[" label "]") " Elapsed time: "
               (format-delta elapsed#)
               " msecs"))
     ret#))
