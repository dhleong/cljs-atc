(ns atc.data.airports
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [ft->m]]
   [atc.engine.model :refer [vec3]]
   [atc.util.numbers :refer [->int]]
   [clojure.string :as str]
   [promesa.core :as p]
   [shadow.lazy :as lazy]))

; NOTE: We explicitly do NOT want to require these namespaces,
; since they should be code-split
#_{:clj-kondo/ignore [:unresolved-namespace]}
(def ^:private airport-loadables
  {:kjfk (lazy/loadable atc.data.airports.kjfk/airport)})

(defn list-airports []
  (->> airport-loadables
       keys
       (map (fn [k]
              {:key k
               :label (str/upper-case (name k))}))))

(defn load-airport [airport-id]
  (if-some [loadable (get airport-loadables airport-id)]
    (if (lazy/ready? loadable)
      @loadable
      (-> ; NOTE: lazy/load *should* return a promise, but it
          ; does not seem to play well with promesa, so...
          (p/create
            (fn [p-resolve p-reject]
              (lazy/load loadable p-resolve p-reject)))
          (p/catch #?(:clj (partial println "[ERROR]")
                      :cljs js/console.error))))
    (throw (ex-info "No such airport: " {:id airport-id}))))

(defn runway-coords [airport runway]
  (when-let [runway-object (->> airport
                                :runways
                                (filter #(or (= runway (:start-id %))
                                             (= runway (:end-id %))))
                                first)]
    (let [elevation (-> (:position airport)
                        (nth 2)
                        ft->m)
          start (-> (local-xy (:start-threshold runway-object) airport)
                    (vec3 elevation))
          end (-> (local-xy (:end-threshold runway-object) airport)
                  (vec3 elevation))]
      (if (= (:start-id runway-object) runway)
        [start end]
        [end start]))))

(defn runway->heading [airport runway]
  (-> (->int runway)
      (* 10)
      (- (:magnetic-north airport))))
