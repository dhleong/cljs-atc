(ns atc.util.testing
  (:require
   [atc.data.airports.kjfk :as kjfk]
   [atc.engine.model :refer [v- vec3 vmag]]
   [atc.subs-util :refer [navaids-by-id]]))

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
                    ; The compiler doesn't believe us, but these
                    ; *should* both be numbers. Let's reassure it!
                    #?(:cljs (- ^number a ^number b)
                       :clj (- a b))
                    (vmag (v- a b)))]
     (<= (abs distance)
         delta))))

(defn create-engine []
  {:airport kjfk/airport
   :game/navaids-by-id (navaids-by-id kjfk/airport)})

