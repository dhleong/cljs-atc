(ns atc.speech.enhanced
  (:require
   ["@diffusionstudio/vits-web" :as tts]
   ["tone" :as Tone]
   [promesa.core :as p]))

 ; NOTE: Sadly this module seems to have some issues for some reason... so we
; use our custom-patched version of vits-web, which performs the same:
; ["@mintplex-labs/piper-tts-web" :as tts]

(def ^:private voice-id  "en_US-libritts_r-medium")
(def ^:private speakers-count 904) ; TODO: use this to select a "voice" (speaker-id)

(defn- predict [{:keys [text speaker-id] :or {speaker-id 0}}]
  (tts/predict #js {:text text
                    :voiceId voice-id
                    :speakerId speaker-id}))

; Imported lazily in atc.speech
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn speak [{:keys [voice message]}]
  (-> (p/let [_ (println "starting up...")
              wav (predict {:text message
                            :speaker-id voice})
              ^js player (Tone/Player. (js/URL.createObjectURL wav))]
        (Tone/loaded)
        (.toDestination player)
        (.start player))
      (p/catch println)))

; Imported lazily in atc.speech
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (p/do!
   (println "[enhanced] init...")
   (Tone/start)
   (predict {:text ""})
   (println "[enhanced] ready!")))
