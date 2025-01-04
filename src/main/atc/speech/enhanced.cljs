(ns atc.speech.enhanced
  (:require
   ["@diffusionstudio/vits-web" :as tts]
   ["tone" :as Tone]
   [applied-science.js-interop :as j]
   [promesa.core :as p]))

 ; NOTE: Sadly this module seems to have some issues for some reason... so we
; use our custom-patched version of vits-web, which performs the same:
; ["@mintplex-labs/piper-tts-web" :as tts]

(def ^:private voice-id  "en_US-libritts_r-medium")
(def ^:private speakers-count 904) ; TODO: use this to select a "voice" (speaker-id)
(def ^:private simulate-radio? true)

(def ^:private max-distance 10000)

(defn- predict [{:keys [text speaker-id] :or {speaker-id 0}}]
  (tts/predict #js {:text text
                    :voiceId voice-id
                    :speakerId speaker-id}))

; TODO: Use this!
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn pick-random-voice []
  (rand-int speakers-count))

(defn- radioify! [^Tone/Player player {:keys [distance]}]
  (let [distance-factor (/ distance max-distance)

        ; Use bandpath to limit the frequency range
        bandpass (Tone/BiquadFilter.
                  #js {; Increasing frequency increases clarity;
                        ; decreasing it makes it more "muffled"
                       :frequency 400,
                        ; Increasing Q makes the sound more "compressed"
                        ; like the source is further away.
                       :Q (+ 0.5 (* 3 distance-factor)),
                       :type "bandpass"})

        ; Add some distortion:
        ; The range "should" be [0, 1] so going over gives us some extra crunch
        distortion (doto (Tone/Distortion. (+ 0.5 distance-factor))
                     (.toDestination))

        ; Modulate filter frequency for a dynamic effect
        ocillation 0.2
        frequency-min 300
        frequency-max 500
        filter-frequency-modulator (doto (Tone/LFO.
                                          ocillation frequency-min frequency-max)
                                     (.start))

        ; Add a little noise during the transmission
        noise (doto (Tone/Noise. "white")
                (.start)
                (.connect
                 (doto (Tone/Gain. (+ 0.015 (* 0.01 distance-factor)))
                   (.connect
                    (.toDestination (Tone/Filter. 200, "highpass"))))))

        ; Add a delay to simulate hesitation after keying the mic
        delay-ms (+ 100 (rand-int 400))
        delay-node (Tone/Delay. (/ delay-ms 1000)) ; ms -> seconds

        promise (p/deferred)

        on-stop (fn on-stop []
                  ; Stop playing the noise when the transmission ends:
                  (.stop noise)
                  (p/resolve! promise))]

    ; NOTE: the player's onstop doesn't account for its output delay, and
    ; the Delay node doesn't have onstop, so we "fake" it here:
    (j/assoc! player :onstop (fn []
                               (js/setTimeout on-stop delay-ms)))

    ; Connect the bandpass frequency modulator:
    (.connect filter-frequency-modulator (.-frequency bandpass))

    ; TODO: Rate? pitch shift?
    (.chain player delay-node bandpass distortion Tone/Destination)

    promise))

; Imported lazily in atc.speech
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn speak [{:keys [voice message radio? distance]
              :or {radio? true
                   distance (/ max-distance 2)}}]
  (-> (p/let [wav (predict {:text message
                            :speaker-id voice})
              ^Tone/Player player (Tone/Player. (js/URL.createObjectURL wav))

              radio? (and simulate-radio? radio?)

              ; NOTE: We wrap in a vec to prevent p/let from awaiting it
              [promise] (if radio?
                          ; TODO: We could compute the distance of the craft to the tower
                          [(radioify! player {:distance distance})]

                          (do (.toDestination player)

                              [(p/create
                                (fn [p-resolve]
                                  (j/assoc! player :onstop p-resolve)))]))]

        (Tone/loaded)
        (.start player)

        ; Return a promise that resolves when the utterence completes
        promise)
      (p/catch (partial js/console.error "[enhanced] error"))))

; Imported lazily in atc.speech
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (p/do!
   (println "[enhanced] init...")
   (Tone/start)
   (predict {:text ""})
   (println "[enhanced] ready!")))
