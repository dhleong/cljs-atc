(ns atc.voice.process
  (:require
   [atc.voice.parsing.callsigns :as callsigns]
   [atc.voice.parsing.instructions :as instructions]
   [atc.voice.parsing.numbers :as numbers]
   [clojure.string :as str]
   [instaparse.core :as insta]))

; TODO: Declaring the grammar in code is convenient, but we will probably want to
; generate it at compile time, dump it to a file, and load that file in production
; instead for performance...
(def fsm
  (delay
    (time
      (insta/parser
        (str/join "\n"
                  (concat
                    instructions/rules
                    callsigns/rules
                    numbers/rules))
        :auto-whitespace :standard))))

(def transformers
  (delay
    (merge
      instructions/transformers
      numbers/transformers
      callsigns/transformers)))

(defn find-command [input]
  (let [output (->> input
                    (@fsm)
                    (insta/transform @transformers))]
    (if (insta/failure? output)
      (println "Unable to parse input: " output)
      output)))

(comment
  (println (find-command "delta one fly heading two zero zero"))
  (println (find-command "november one two two fly heading zero two zero contact center good day"))
  (println (find-command "piper eight one zero two contact tower"))
  (println (find-command "speed bird two twenty one climb maintain flight level two two zero"))
  (println (find-command "speed bird two twenty one descend and maintain one two thousand")))
