(ns atc.engine.aircraft.instructions
  (:require
   [clojure.string :as str]))

(defmulti dispatch-instruction (fn [_craft _context [instruction]] instruction))

(defmethod dispatch-instruction
  :steer
  [craft _ [_ heading steer-direction]]
  (let [heading-str (-> heading
                        (str)
                        (str/split #"")
                        (next)
                        (as-> numbers
                          ; TODO 9 -> niner, etc.
                          (str/join " " numbers)))]
    (-> craft
        (update ::utterance-parts conj [(when steer-direction (name steer-direction))
                                        {:pronunciation heading-str
                                         :text heading}])

        (update :commands dissoc :direct)
        (update :commands assoc :heading heading :steer-direction steer-direction))))

(defmethod dispatch-instruction
  :direct
  [craft context [_ fix-id]]
  (if-let [fix (get-in context [:game/navaids-by-id fix-id])]
    (-> craft
        (update ::utterance-parts conj ["direct " fix])
        (update :commands assoc :heading)
        (update :commands assoc :direct fix))

    ; TODO: Handle GA aircraft without the fix
    (-> craft
        (update ::utterance-parts conj "unable direct"))))

(defmethod dispatch-instruction
  :default
  [craft _context [instruction]]
  (println "TODO: Unhandled craft instruction: " instruction)
  craft)
