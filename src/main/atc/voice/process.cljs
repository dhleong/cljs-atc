(ns atc.voice.process
  (:require
   [atc.voice.parsing.callsigns :as callsigns]
   [atc.voice.parsing.instructions :as instructions]
   [atc.voice.parsing.letters :as letters]
   [atc.voice.parsing.numbers :as numbers]
   [clojure.string :as str]
   [instaparse.core :as insta]))

; TODO: Declaring the grammar in code is convenient, but we will probably want to
; generate it at compile time, dump it to a file, and load that file in production
; instead for performance...
(def ^:private fsm
  (delay
    (time
      (insta/parser
        (str/join "\n"
                  (concat
                    instructions/rules
                    letters/rules
                    callsigns/rules
                    numbers/rules))
        :auto-whitespace :standard))))

(def ^:private transformers
  (delay
    (merge
      instructions/transformers
      letters/transformers
      numbers/transformers
      callsigns/transformers)))

(defn- parse-input [input]
  (insta/parse @fsm input :total true))

(defn grammar []
  (:grammar @fsm))

(defn find-command [input]
  (let [output (->> input
                    (parse-input)

                    ; NOTE: clj-kondo seems confused but this fn definitely exists!
                    #_{:clj-kondo/ignore [:unresolved-var]}
                    (insta/transform @transformers))]
    (if (insta/failure? output)
      (let [failure (insta/get-failure output)
            {:keys [callsign instructions]} output
            instructions (vec instructions)
            without-trailing-standby (if (= [:standby] (peek instructions))
                                       (pop instructions)
                                       instructions)]
        (println "Unable to fully parse input: " failure)
        {:callsign callsign
         :instructions (conj without-trailing-standby [:error failure])})
      output)))

(comment
  (println (find-command "delta one"))
  (println (parse-input "delta one fly heading two zero three have fun"))
  (find-command "delta one have fun fly heading two zero three")
  (println (find-command "delta one fly heading two zero zero"))
  (println (find-command "november one two two fly heading zero two zero contact center good day"))
  (println (find-command "piper eight one zero two contact tower"))
  (println (find-command "speed bird two twenty one climb maintain flight level two two zero"))
  (println (find-command "speed bird two twenty one descend and maintain one two thousand")))
