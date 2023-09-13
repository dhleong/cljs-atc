(ns atc.engine.aircraft.commands.helpers)

; ======= Radiology =======================================

(defn build-utterance-from [craft]
  (assoc (:pilot craft)
         :name (:callsign craft)))

(defn utter-once [craft & parts]
  (when (seq parts)
    {:message (concat ["center," craft ";"] parts)
     :from (build-utterance-from craft)}))
