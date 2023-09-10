(ns atc.voice.pretty
  (:require
   [clojure.string :as str]))

(def ^:private voice-partial-replacements
  {"ay tis" "ATIS"})

(def ^:private voice-partial-replacements-regex
  (re-pattern
    (str/join "|" (keys voice-partial-replacements))))

(defn cleanup-spoken-text [text]
  (-> text
      (str/replace voice-partial-replacements-regex voice-partial-replacements)))
