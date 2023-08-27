(ns atc.util.testing
  (:require
   [atc.engine.model :refer [v- vec3 vmag]]))

(defn- maybe->vec3
  "Coerce v into a vec3, if it looks vec3-like"
  [v]
  (if (some? (:x v))
    (vec3 v)
    v))

(defn roughly=
  ([a b] (roughly= a b :delta 0.1))
  ([a b & {:keys [delta]}]
   (let [a (maybe->vec3 a)
         b (maybe->vec3 b)
         distance (if (number? a)
                    ; The compiler doens't believe us, but these
                    ; *should* both be numbers. Let's reassure it!
                    (- ^number a ^number b)
                    (vmag (v- a b)))]
     (<= (abs distance)
         delta))))
