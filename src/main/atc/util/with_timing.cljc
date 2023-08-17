(ns atc.util.with-timing)

(defn now []
  #? (:cljs (js/Date.now)
      :clj (System/nanoTime)))

(defmacro with-timing [label expr]
  `(let [start# (now)
         ret# ~expr
         elapsed# (- (now) start#)]
     (prn (str ~(str "[" label "]") " Elapsed time: "
               #? (:cljs (.toFixed elapsed# 6)
                   :clj (/ (double elapsed#) 1000000.0))
               " msecs"))
     ret#))
