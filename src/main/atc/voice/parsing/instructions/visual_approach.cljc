(ns atc.voice.parsing.instructions.visual-approach
  (:require
   [atc.util.instaparse :refer-macros [defrules]]))

(defrules visual-approach-instruction-rules
  "report-field-in-sight =
       (rough-direction number <'miles report'> <visual-field> <'in sight'>)
     | (<'report'> <visual-field> <'in sight'> (number <'miles'>)?)
     | (number <'miles from'> <navaid-pronounced> <'airport'>? <'report'> <visual-field> <'in sight'>)
   "
  [:navaid-pronounced :number :rough-direction :visual-field])

(defrules visual-approach-support-rules
  "rough-direction = number <'o clock'>
   <visual-field> = 'field' | 'airport'"
  [:number])

(def visual-approach-rules
  (merge-with merge
              visual-approach-instruction-rules
              visual-approach-support-rules))
