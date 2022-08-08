(ns atc.voice.process
  (:require
   [atc.voice.parsing.callsigns :as callsigns]
   [atc.voice.parsing.instructions :as instructions]
   [atc.voice.parsing.letters :as letters]
   [atc.voice.parsing.numbers :as numbers]
   [clojure.string :as str]
   [instaparse.core :as insta]))

(def builtin-rules (concat
                     instructions/rules
                     letters/rules
                     callsigns/rules
                     numbers/rules))

(def builtin-transformers (merge
                            instructions/transformers
                            letters/transformers
                            numbers/transformers
                            callsigns/transformers))

(defn build-machine [airport-context]
  {:fsm (time
          (insta/parser
            (str/join "\n" (concat builtin-rules (:rules airport-context)))
            :auto-whitespace :standard))
   :transformers (merge builtin-transformers (:transformers airport-context))})

(defn- parse-input [machine input]
  (insta/parse (:fsm machine) input :total true))

(defn find-command [machine input]
  (let [output (->> input
                    (parse-input machine)

                    ; NOTE: clj-kondo seems confused but this fn definitely exists!
                    #_{:clj-kondo/ignore [:unresolved-var]}
                    (insta/transform (:transformers machine)))]
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
