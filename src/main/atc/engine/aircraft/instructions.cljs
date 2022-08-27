(ns atc.engine.aircraft.instructions)

(defn- utter [craft & utterance]
  (update craft ::utterance-parts conj utterance))

(defmulti dispatch-instruction (fn [_craft _context [instruction]] instruction))

(defmethod dispatch-instruction
  :cleared-approach
  [craft context [_ approach-type runway]]
  (cond
    (not (contains? (:game/airport-runway-ids context) runway))
    (utter craft "unable" runway)

    (contains? #{:visual :rnav} approach-type)
    (utter craft "unable" (name approach-type))

    :else (-> craft
            (assoc :state :cleared-approach)
            (utter "cleared approach runway" [:runway runway]))))

(defmethod dispatch-instruction
  :steer
  [craft _ [_ heading steer-direction]]
  (-> craft
      (utter (when steer-direction (name steer-direction))
             [:heading heading])

      (update :commands dissoc :direct)
      (update :commands assoc :heading heading :steer-direction steer-direction)))

(defmethod dispatch-instruction
  :direct
  [craft context [_ fix-id]]
  (if-let [fix (get-in context [:game/navaids-by-id fix-id])]
    (-> craft
        (utter "direct " fix)
        (update :commands assoc :heading)
        (update :commands assoc :direct fix))

    ; TODO: Handle GA aircraft without the fix
    (-> craft
        (utter "unable direct"))))

(defmethod dispatch-instruction
  :default
  [craft _context [instruction]]
  (println "TODO: Unhandled craft instruction: " instruction)
  craft)