(ns atc.pronunciation
  (:require
   [atc.vosk.fst :as fst :refer [skip-to-fst-file]]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.util.zip GZIPInputStream)
   (org.apache.commons.compress.archivers.tar TarArchiveInputStream)))


; ======= Pronouncability checking ========================

(defn- load-dictionary-checker []
  (let [model-file (io/file "public/voice-model.tar.gz")]
    (with-open [file-in (io/input-stream model-file)
                gzip-stream (GZIPInputStream. file-in)
                tar-stream (TarArchiveInputStream. gzip-stream)]
      (skip-to-fst-file tar-stream)
      (let [valid-words (fst/read-valid-words tar-stream)]
        (fn missing-words [input]
          (let [parts (-> input
                          (str/lower-case)
                          (str/split #"\W+"))]
            (->> parts
                 (keep (fn [word]
                         (when-not (contains? valid-words word)
                           word)))
                 seq)))))))

(def ^:private dictionary-checker
  (delay
    (load-dictionary-checker)))

(defn missing-words [word]
  (when word
    (@dictionary-checker word)))

(defn pronounceable? [word]
  (nil? (missing-words word)))

(defn unpronounceable [candidates]
  (->> candidates
       (pmap #(when-some [missing (missing-words %)]
                [% missing]))
       (keep identity)))

; ======= Pronunciation generation ========================

(def common-replacements
  {"ville" "fill"})

(defn- split-words [word]
  (for [i (range 2 (dec (count word)))]
    (let [a (subs word 0 i)
          b (subs word i)]
      [(get common-replacements a a)
       (get common-replacements b b)])))

(defn- check-split-words [word]
  (->> (split-words word)
       (pmap (fn [pair]
               (let [new-word (str/join " " pair)]
                 (when-not (missing-words new-word)
                   new-word))))
       (keep identity)
       first))

(defn make-pronounceable [word]
  (or (check-split-words word)
      word))

(comment
  (time (missing-words "deer park"))
  (missing-words "la guardia")
  (missing-words "rockdale")
  (missing-words "wilkes barre"))
