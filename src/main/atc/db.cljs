(ns atc.db
  (:require
   [atc.structures.rolling-history :refer [rolling-history]]))

(def max-game-snapshots 60) ; ~4 minutes

(def default-db
  ; NOTE: default to game, for now
  {:page [:game]

   :game-history (rolling-history max-game-snapshots)
   :game-events []

   ; Vec of {:speaker str, :text str, :self? bool}
   :radio-history []

   :speech {:available? nil
            :speaking? false
            :queue #queue []}})

