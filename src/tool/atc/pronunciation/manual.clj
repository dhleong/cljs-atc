(ns atc.pronunciation.manual
  "A collection of manually-defined navaid pronunciations that
  cannot be easily derived from the input")

(def manually-defined-pronunciations
  {"kennebunk" "kenney bunk"
   ; NOTE: Sometimes just "fah zoo" works better with TTS... Perhaps we can support alternates at some point...
   "fzool" "fah zoo el"
   "rngrr" "ranger"})
