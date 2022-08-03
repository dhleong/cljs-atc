(ns atc.voice.grammar)

(defn generate []
  (let [digits ["zero" "one" "two" "three" "four" "five" "six" "seven" "eight" "nine"]
        double-digits (concat
                        ["ten" "eleven" "twelve" "thirteen" "fourteen" "fifteen" "sixteen" "seventeen" "eighteen" "nineteen"]
                        (for [ten ["twenty" "thirty" "forty" "fifty" "sixty" "seventy" "eighty" "ninety"]
                              d digits]
                          (str ten " " d)))

        airlines ["delta" "speed bird" "united"]
        airline-callsigns (concat
                            (for [a airlines
                                  d digits
                                  dd double-digits]
                              (str a " " d " " dd))

                            (for [a airlines
                                  d1 double-digits
                                  d2 double-digits]
                              (str a " " d1 " " d2)))]

    (->> (concat
           airline-callsigns
           ["contact tower"]
           ["turn left heading two zero zero"]
           ["[unk]"])
         to-array
         (js/JSON.stringify))))
