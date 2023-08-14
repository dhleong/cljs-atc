(ns atc.voice.process
  (:require
   [atc.util.instaparse :as instaparse]
   [atc.util.with-timing :refer-macros [with-timing]]
   [atc.voice.parsing.callsigns :as callsigns]
   [atc.voice.parsing.instructions :as instructions]
   [atc.voice.parsing.letters :as letters]
   [atc.voice.parsing.navaids :as navaids]
   [atc.voice.parsing.numbers :as numbers]
   [clojure.string :as str]
   [instaparse.core :as insta]))

(def builtin-rules (merge-with merge
                     #_instructions/rules
                     navaids/rules
                     letters/rules
                     callsigns/rules
                     numbers/rules))

(def builtin-transformers (merge
                            instructions/transformers
                            navaids/transformers
                            letters/transformers
                            numbers/transformers
                            callsigns/transformers))

(defn build-machine [airport-context]
  {:fsm (with-timing "build-machine"
          (-> (insta/parser
                (str/join "\n" (concat instructions/rules
                                       (:rules airport-context)
                                       (instaparse/generate-grammar-nops
                                         (keys (:grammar builtin-rules)))))
                :auto-whitespace :standard)
              (update :grammar merge builtin-rules)))
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
