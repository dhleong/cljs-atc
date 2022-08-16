(ns atc.okay
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io EOFException InputStream)
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

    (satisfies? io/IOFactory x)
    (-as-buffered-source (io/input-stream x))

    :else
    (throw (ex-info "Unable to convert input to a buffered source"
                    {:input x}))))


; ======= Record value parsers ============================

(defn with-bytes-count [bytes-count]
  (fn [f]
    (vary-meta f assoc :bytes-count bytes-count)))

(defn- with-bytes-count-from [f]
  (fn [parser]
    ((with-bytes-count (-> f meta :bytes-count))
     parser)))

(defn compose
  "Exactly as (comp), but forwards important metadata"
  ([f g] ((with-bytes-count-from g) (comp f g)))
  ([f g h] ((with-bytes-count-from h) (comp f g h)))
  ([f g h & fs] ((with-bytes-count-from (last fs)) (apply comp f g h fs))))

(defn justified-string
  ([bytes-length] (justified-string bytes-length nil))
  ([bytes-length charset]
   (let [charset-obj (if (some? charset)
                       (Charset/forName charset)
                       (Charset/defaultCharset))]
     ((with-bytes-count bytes-length)
      (fn [^Buffer buffer]
        (str/trim (.readString buffer bytes-length charset-obj)))))))

(defn optional-string [parser]
  ((with-bytes-count-from parser)
    (fn optional [v]
     (when-not (str/blank? v)
       (parser v)))))

(defn justified-string-number [str->number bytes-length & {:keys [optional?]}]
  (let [f #(try (str->number %)
                (catch Throwable cause
                  (throw (ex-info (str "Failed to parse `" % "` into a number")
                                  {:value %}
                                  cause))))
        string-reader (justified-string bytes-length)]
    (compose
      (if optional?
        (optional-string f)
        f)
      string-reader)))

(def justified-int (partial justified-string-number #(Long/parseLong %)))
(def justified-float (partial justified-string-number #(Double/parseDouble %)))

(defn ignore-bytes [bytes-count]
  ((with-bytes-count bytes-count)
    (fn ignore-bytes
      ([^Buffer frame]
       (.skip frame bytes-count)
       ::ignored-value)
      ([output ^Buffer frame]
       (ignore-bytes frame)
       output))))

; ======= Record reading ==================================

(defn fixed-record-length [record]
  (or (-> record meta :bytes-count)

      (when (sequential? record)
        (let [sizes (->> record
                         (map #(if-let [c (:bytes-count (meta %))]
                                 c
                                 (throw (ex-info "Size data not available on record part"
                                                 {:record record
                                                  :part %
                                                  :meta (meta %)})))))]
          (when (every? some? sizes)
            (apply + sizes))))))

(defn read-record
  ([record ^Buffer frame] (read-record record frame {}))
  ([record ^Buffer frame initial-output]
   (reduce
     (fn [output record-part]
       (record-part output frame))
     initial-output
     record)))

(defn fixed-frame-producer [frame-length-bytes in]
  (let [source (buffered-source in)
        frame (Buffer.)]
    (fn produce-frame ^Buffer []
      (try
        (.clear frame)
        (.readFully source frame frame-length-bytes)
        (.copy frame)
        (catch EOFException _
          nil)))))

(defn fixed-frame-sequence [frame-length-bytes in]
  (->> (repeatedly (fixed-frame-producer frame-length-bytes in))
       (take-while some?)))

(defn- require-fixed-record-length [record]
  (if-let [frame-length-bytes (fixed-record-length record)]
    frame-length-bytes
    (throw (ex-info "Provided record does not have a fixed length"
                    {:record record}))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn fixed-record-sequence
  "Returns a lazy sequence that reads records with a fixed length in bytes
  from `in`, which should be (or be convertable to) a buffered-source."
  ([record in]
   (fixed-record-sequence record (require-fixed-record-length record) in))

  ([record frame-length-bytes in]
   (->> (fixed-frame-sequence frame-length-bytes in)
        (map (partial read-record record)))))

(defn search-for-fixed-record [in record k pred]
  (let [[key-offset key-reader] (reduce
                                  (fn [offset part]
                                    (if (= k (:key (meta part)))
                                      (reduced [offset part])
                                      (+ offset (:bytes-count (meta part)))))
                                  0
                                  record)
        frame-length-bytes (require-fixed-record-length record)]
    (loop [frames (fixed-frame-sequence frame-length-bytes in)]
      (when-let [^Buffer current (first frames)]
        (let [for-key (doto (.copy current)
                        (.skip key-offset))
              read-value (key-reader {} for-key)
              key-value (get read-value k ::not-found)]
          (when (= key-value ::not-found)
            (throw (ex-info "Illegal state: key-reader did not produce a value"
                            {:reader key-reader
                             :key k
                             :produced read-value})))
          (if (pred key-value)
            (cons current frames)
            (recur (next frames))))))))


; ======= Record compliation ==============================

(defn compile-record-part [part]
  (cond
    (vector? part) (let [[k reader] part]
                     (vary-meta
                       ((with-bytes-count-from reader)
                        (fn read-part [output ^Buffer frame]
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
                       assoc :key k))

    ; TODO wrap with exception handler
    (fn? part) part

    :else (throw (ex-info (str "Unexpected record part:" part) {:part part}))))

(defn compile-record [& parts]
  (map compile-record-part parts))
