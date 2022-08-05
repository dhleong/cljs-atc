(ns atc.db)

(def default-db
  ; NOTE: default to game, for now
  {:page [:game]

   :speech {:available? nil
            :speaking? false
            :queue #queue []}})

