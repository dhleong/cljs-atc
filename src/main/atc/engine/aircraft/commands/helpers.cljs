(ns atc.engine.aircraft.commands.helpers
  (:require
   [atc.data.units :refer [ft->m]]
   [atc.engine.model :refer [vec3]]))

(defn normalize-heading [h]
  (if (< h 0)
    (+ h 360)
    (mod h 360)))

(defn primary-airport-position [airport]
  (vec3 0 0 (ft->m (last (:position airport)))))

; ======= Radiology =======================================

(defn build-utterance-from [craft]
  (assoc (:pilot craft)
         :name (:callsign craft)))

(defn message-from [craft message]
  {:message message
   :craft craft
   :from (build-utterance-from craft)})

(defn utter-once [craft & parts]
  (when (seq parts)
    (message-from craft (concat ["center," craft ";"] parts))))

; ======= Building blocks =================================

(defn apply-rate [aircraft command-key path rate-keys from commanded-value dt]
  (let [sign (if (> commanded-value from) 1 -1)
        rate-key (rate-keys sign)
        rate (get-in aircraft [:config rate-key])
        new-speed (+ from (* sign rate dt))]
    (if (<= (abs (- commanded-value new-speed))
            (* rate 0.5))
      (-> aircraft
          (assoc-in path commanded-value)  ; close enough; snap to
          (update :commands dissoc command-key))

      (-> aircraft
          (assoc-in path new-speed)))))
