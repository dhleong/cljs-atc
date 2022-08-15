(ns atc.okay
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io InputStream)
   (java.nio.charset Charset)
   (okio
     Buffer
     BufferedSource
     Okio
     Source)))


; ======= Source coercion =================================

(defprotocol IIntoBufferedSource
  (-as-buffered-source [this]))

(extend-protocol IIntoBufferedSource
  BufferedSource
  (-as-buffered-source [this] this)
  Buffer
  (-as-buffered-source [this] this)

  Source
  (-as-buffered-source [this] (Okio/buffer this))

  InputStream
  (-as-buffered-source [this] (Okio/buffer (Okio/source this))))

(defn buffered-source [x]
  (cond
    (satisfies? IIntoBufferedSource x)
    (-as-buffered-source x)

    (satisfies? io/IOFactory)
    (-as-buffered-source (io/input-stream x))

    :else
    (throw (ex-info "Unable to convert input to a buffered source"
                    {:input x}))))


; ======= content =========================================

(defn compile-record-part [part]
  (cond
    (vector? part) (let [[k reader] part]
                     (fn [output frame]
                       (try
                         (let [read-value (reader frame)]
                           (if-not (= ::ignored-value read-value)
                             (assoc output k read-value)
                             output))
                         (catch Throwable cause
                           (throw (ex-info (str "Failed to read " k)
                                           {:frame frame
                                            :output output}
                                           cause))))))

    ; TODO wrap with exception handler
    (fn? part) part

    :else (throw (ex-info (str "Unexpected record part:" part) {:part part}))))

(defn compile-record [& parts]
  (map compile-record-part parts))


; ======= Record value parsers ============================

(defn justified-string
  ([bytes-length] (justified-string bytes-length nil))
  ([bytes-length charset]
   (let [charset-obj (if (some? charset)
                       (Charset/forName charset)
                       (Charset/defaultCharset))]
     #(str/trim (.readString % bytes-length charset-obj)))))

(defn optional-string [parser]
  (fn optional [v]
    (when-not (str/blank? v)
      (parser v))))

(defn justified-string-number [str->number bytes-length & {:keys [optional?]}]
  (let [f #(try (str->number %)
                (catch Throwable cause
                  (throw (ex-info (str "Failed to parse `" % "` into a number")
                                  {:value %}
                                  cause))))]
    (comp
      (if optional?
        (optional-string f)
        f)
      (justified-string bytes-length))))

(def justified-int (partial justified-string-number #(Long/parseLong %)))
(def justified-float (partial justified-string-number #(Double/parseDouble %)))

(defn ignore-bytes [bytes-count]
  (fn ignore-bytes
    ([^Buffer frame]
     (.skip frame bytes-count)
     ::ignored-value)
    ([output ^Buffer frame]
     (ignore-bytes frame)
     output)))

; ======= Record reading ==================================

(defn read-record
  ([record frame] (read-record record frame {}))
  ([record frame initial-output]
   (reduce
     (fn [output record-part]
       (record-part output frame))
     initial-output
     record)))

(defn fixed-record-sequence
  "Returns a lazy sequence that reads records with a fixed length in bytes
  from `in`, which should be (or be convertable to) a buffered-source."
  [record frame-length-bytes in]
  (let [source (buffered-source in)
        frame (Buffer.)
        read-next (fn read-next []
                    (.clear frame)
                    (.readFully source frame frame-length-bytes)
                    (read-record record frame))
        read-seq (fn read-seq []
                   (cons (read-next)
                         (lazy-seq (read-seq))))]
    (read-seq)))
