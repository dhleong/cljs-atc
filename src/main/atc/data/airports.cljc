(ns atc.data.airports
  (:require
   [atc.data.core :refer [local-xy]]
   [atc.data.units :refer [ft->m]]
   [atc.engine.model :refer [vec3]]
   [atc.util.lazy :as lazy]
   [atc.util.numbers :refer [->int]]
   [clojure.string :as str]
   [promesa.core :as p]))

; NOTE: We explicitly do NOT want to require these namespaces,
; since they should be code-split
(def ^:private airport-loadables
  {:kjfk (lazy/dynamic-import 'atc.data.airports.kjfk/exports)})

(defn list-airports []
  (->> airport-loadables
       keys
       (map (fn [k]
              {:key k
               :label (str/upper-case (name k))}))))

(defn load-airport [airport-id]
  (if-some [loadable (get airport-loadables airport-id)]
    (-> (p/let [{:keys [airport]} (lazy/unpack loadable)]
          airport)
        (p/catch #?(:clj (partial println "[ERROR]")
                    :cljs js/console.error)))
    (throw (ex-info "No such airport: " {:id airport-id}))))

(defn airport-parsing-rules [airport-id]
  (:navaid-pronounced
   @(get airport-loadables airport-id)))

(defn airport-parsing-transformers [airport-id]
  {:navaid-pronounced
   (:navaids-by-pronunciation
    @(get airport-loadables airport-id))})

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

(defn runway->heading [_airport runway]
  (-> (->int runway)
      (* 10)))
