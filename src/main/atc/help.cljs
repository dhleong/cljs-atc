(ns atc.help
  (:require
   [atc.data.airlines :refer [all-airlines]]
   [atc.radio :refer [->speakable]]
   [clojure.string :as str]))

(defn- with-callsign [desc callsign]
  (let [[airline-id] (str/split callsign #"[0-9]+")
        airline (get all-airlines airline-id)]
    (cond-> desc
      (some? airline) (str ". This one is pronounced \""
                           (:radio-name airline)
                           "\""))))

(defn- with-fix [engine desc fix-id]
  (let [fix (get-in engine [:game/navaids-by-id fix-id])
        fix-name (or (:name fix)
                     (:id fix))
        speakable (->speakable [fix])]
    (cond-> desc
      (some? fix) (str ". This one is " fix-name)

      (not= fix-name speakable)
      (str ", pronounced " speakable))))

(defn build-help [engine part content]
  (case part
    :active-runways-primary "The active arrival / departure runways at the primary airport"
    :active-arrival-runways-primary "The active arrival runways at the primary airport"
    :active-departure-runways-primary "The active departure runways at the primary airport"
    :altitude-assignments "A list of altitude assignments for this craft in 100's of feet"
    :arrival-fix (with-fix
                   engine
                   "The fix through which this aircraft will arrive in our airspace"
                   content)
    :atis "Current ATIS code."
    :callsign (with-callsign
                "The aircraft's callsign"
                content)
    :craft-type "The aircraft's type identifier"
    :cruise-flight-level "The altitude (as Flight Level) to which the craft eventually should climb"
    :departure-fix (with-fix
                     engine
                     "The fix from which this aircraft will depart our airspace"
                     content)
    :destination "The aircraft's destination"
    :origin "The airport from which this aircraft departed"
    :primary-altimeter "Current altimeter setting at the primary airport"
    :route "The aircraft's route"
    :squawk "The aircraft's assigned \"squawk\" identifier code"
    :wind "Wind direction and speed in kts"
    :visibility-sm "Visibility in statute miles"
    :weather-time "UTC date/time when the weather was updated"))
