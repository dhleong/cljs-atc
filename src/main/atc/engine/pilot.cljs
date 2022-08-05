(ns atc.engine.pilot
  (:require
   [atc.speech :as speech]))

(defn generate [voice]
  {:voice (or voice
              (speech/pick-random-voice))})
