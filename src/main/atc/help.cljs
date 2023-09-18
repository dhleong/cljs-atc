(ns atc.help)

(defn build-help [part]
  (case part
    :active-runways-primary "The active arrival / departure runways at the primary airport"
    :active-arrival-runways-primary "The active arrival runways at the primary airport"
    :active-departure-runways-primary "The active departure runways at the primary airport"
    :altitude-assignments "A list of altitude assignments for this craft in 100's of feet"
    :arrival-fix "The fix through which this aircraft will arrive in our airspace"
    :atis "Current ATIS code."
    :callsign "The aircraft's callsign"
    :craft-type "The aircraft's type identifier"
    :cruise-flight-level "The altitude (as Flight Level) to which the craft eventually should climb"
    :departure-fix "The fix from which this aircraft will depart our airspace"
    :destination "The aircraft's destination"
    :origin "The airport from which this aircraft departed"
    :primary-altimeter "Current altimeter setting at the primary airport"
    :route "The aircraft's route"
    :squawk "The aircraft's assigned \"squawk\" identifier code"
    :wind "Wind direction and speed in kts"
    :visibility-sm "Visibility in statute miles"
    :weather-time "UTC date/time when the weather was updated"))
