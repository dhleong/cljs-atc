(ns atc.vosk.fst
  (:require
   [atc.okay :as okay :refer [compile-record]]))

(def ^:private fst-file-name "model/graph/Gr.fst")
(def ^:private flag-has-input-table 0x01)
(def ^:private flag-has-output-table 0x02)
(def ^:private supported-fst-version 4)

(defn skip-to-fst-file [tar-stream]
  (loop []
    (if-let [next-entry (.getNextEntry tar-stream)]
      (if (= fst-file-name (.getName next-entry))
        next-entry
        (recur))

      (throw (ex-info "Could not find Gr.fst in model.tar.gz" {})))))

(def string (okay/prefixed-string okay/integer-le))
(def unsigned-int okay/integer-le)
(def unsigned-long okay/long-integer-le)

(def fst-header
  (compile-record
    [:magic-number okay/integer-le]
    [:fst-type string]
    [:arc-type string]
    [:version unsigned-int]
    [:flags unsigned-int]
    [:properties unsigned-long]
    [:start okay/long-integer-le]
    [:numstates okay/long-integer-le]
    [:numarcs okay/long-integer-le]))

(def ^:private symbol-table-header
  (compile-record
    [:magic-number okay/integer-le]
    [:name string]
    [:available-key okay/long-integer-le]
    [:size okay/long-integer-le]))

(defn- read-symbol-table [in]
  (let [header (okay/read-record symbol-table-header in)]
    (->> (for [_ (range (:size header))]
           (let [s (string in)]
             ; This is the symbol key:
             (okay/long-integer-le in)
             s))
         (into #{}))))

(defn- read-fst [in]
  (let [header (okay/read-record fst-header in)]
    (when-not (= supported-fst-version (:version header))
      (throw (ex-info (str "Unexpected FST header version: " (:version header))
                      {:header header})))
    {:header header
     :input (when (not= 0 (bit-and (:flags header)
                                   flag-has-input-table))
              (read-symbol-table in))
     :output (when (not= 0 (bit-and (:flags header)
                                    flag-has-output-table))
               (read-symbol-table in))}))

(defn read-valid-words [in]
  ; NOTE: For our purposes, input and output are identical...
  (:input (read-fst (okay/buffered-source in))))

(comment
  #_:clj-kondo/ignore
  (let [model-file (clojure.java.io/file "public/voice-model.tar.gz")]
    (with-open [file-in (clojure.java.io/input-stream model-file)
                gzip-stream (java.util.zip.GZIPInputStream. file-in)
                tar-stream (org.apache.commons.compress.archivers.tar.TarArchiveInputStream. gzip-stream)]
      (skip-to-fst-file tar-stream)
      (count
        (read-valid-words
          (okay/buffered-source tar-stream))))))
