(ns atc.pronunciation
  (:require
   [atc.okay :as okay]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.util.zip GZIPInputStream)
   (org.apache.commons.compress.archivers.tar TarArchiveInputStream)))

(def ^:private fst-file-name "model/graph/Gr.fst")

(defn- skip-to-fst-file [tar-stream]
  (loop []
    (if-let [next-entry (.getNextEntry tar-stream)]
      (if (= fst-file-name (.getName next-entry))
        next-entry
        (recur))

      (throw (ex-info "Could not find Gr.fst in model.tar.gz" {})))))

(defn- load-dictionary-checker []
  (let [model-file (io/file "public/voice-model.tar.gz")]
    (with-open [file-in (io/input-stream model-file)
                gzip-stream (GZIPInputStream. file-in)
                tar-stream (TarArchiveInputStream. gzip-stream)]
      (skip-to-fst-file tar-stream)
      (let [data (-> (okay/buffered-source tar-stream)
                     (.readUtf8))]
        (fn missing-words [input]
          (let [parts (-> input
                          (str/lower-case)
                          (str/split #"\W+"))]
            (->> parts
                 (keep
                   (fn [word]
                     ; NOTE: This is terribly hacky... but it works
                     (when-not (re-find (re-pattern (str "\\b" word "[^a-z]")) data)
                       word)))
                 seq)))))))

(def ^:private dictionary-checker
  (delay
    (load-dictionary-checker)))

(defn missing-words [word]
  (when word
    (@dictionary-checker word)))

(defn unpronounceable [candidates]
  (->> candidates
       (pmap #(when-some [missing (missing-words %)]
                [% missing]))
       (keep identity)))

(comment
  (time (missing-words "deer park"))
  (missing-words "la guardia")
  (missing-words "rockdale")
  (missing-words "wilkes barre"))
