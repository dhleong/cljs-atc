(ns atc.voice.process
  (:require
   [atc.data.airports :refer [airport-parsing-rules
                              airport-parsing-transformers]]
   [atc.util.with-timing :refer-macros [with-timing]]
   [atc.voice.parsing.callsigns :as callsigns]
   [atc.voice.parsing.instructions :as instructions]
   [atc.voice.parsing.letters :as letters]
   [atc.voice.parsing.navaids :as navaids]
   [atc.voice.parsing.numbers :as numbers]
   [clojure.string :as str]
   [instaparse.core :as insta]))

(def builtin-rules (->> [instructions/rules
                         navaids/rules
                         callsigns/rules
                         letters/rules
                         numbers/rules]
                        (map :grammar)
                        (apply merge)))

(def builtin-transformers (merge
                            instructions/transformers
                            navaids/transformers
                            letters/transformers
                            numbers/transformers
                            callsigns/transformers))

(defn build-machine
  ([airport] (let [kw (-> (:id airport)
                          str/lower-case
                          keyword)]
               (build-machine (airport-parsing-rules kw)
                              (airport-parsing-transformers kw))))
  ([airport-rules airport-transformers]
   {:fsm (with-timing "build-machine"
           (-> (merge
                 builtin-rules
                 (:grammar airport-rules))
               (insta/parser :start :command
                             :auto-whitespace :standard)))
    :transformers (merge builtin-transformers
                         airport-transformers)}))

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
