(ns atc.util.with-timing)

(defmacro with-timing [label expr]
  `(let [start# (js/Date.now)
         ret# ~expr]
     (prn (cljs.core/str ~(str "[" label "]") " Elapsed time: "
                         (.toFixed (- (js/Date.now) start#) 6)
                         " msecs"))
     ret#))
