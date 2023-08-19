(ns atc.util.subs)

(defn get-or-identity
  ([v] v)
  ([v k] (get v k))
  ([v k default] (get v k default)))
