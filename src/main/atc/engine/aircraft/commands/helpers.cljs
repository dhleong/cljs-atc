(ns atc.engine.aircraft.commands.helpers
  (:require
   [atc.data.units :refer [ft->m]]
   [atc.engine.model :refer [vec3]]))

(defn primary-airport-position [airport]
  (vec3 0 0 (ft->m (last (:position airport)))))

; ======= Radiology =======================================

(defn build-utterance-from [craft]
  (assoc (:pilot craft)
         :name (:callsign craft)))

(defn utter-once [craft & parts]
  (when (seq parts)
    {:message (concat ["center," craft ";"] parts)
     :from (build-utterance-from craft)}))
