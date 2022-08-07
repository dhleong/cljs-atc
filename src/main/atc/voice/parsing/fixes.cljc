(ns atc.voice.parsing.fixes)

; NOTE: Ideally we would like to limit these to the ones
; actually present in the loaded map. That would not allow us to
; precompile the grammar, but... we're not doing that right now anyway
; so maybe it's fine?

(def vor-values
  {"kennedy" "JFK"
   "soul berg" "SBJ"})

(def fix-values
  {"merit" "MERIT"
   "gail" "GAYEL"
   "haze" "HAAYS"
   "dodgers" "DOGRS"})
