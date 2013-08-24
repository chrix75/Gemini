(ns gemini.extended
  (:require [gemini.core :refer [def-matching-env rule]]
            [clojure.string :as str]))

(defn analyze-expressions-likeness
  "Takes 2 expressions and calls the matching function matching function f on expressions words.
   Each word of the expression e1 is compared to every word of the expression e2.

   This function returns a lazy sequence of maps. Each item corresponds to a matching word.
   The map has the following data:
     :w1 > The word of the expression e1
     :w2 > The word of the expression e2
     :likeness > The level of words likeness. If likeness is = then there is no error,
                 if likeness is ~ then errors were found."
  [e1 e2 f]
  (when (and (seq e1) (seq e2))
    (filter #(not (nil? %))
            (for [w1 (str/split e1 #" ") w2 (str/split e2 #" ")]
              (if (= w1 w2)
                {:w1 w1 :w2 w2 :likeness "="}
                (if (and (= (first w1) (first w2)) (f w1 w2))
                  {:w1 w1 :w2 w2 :likeness "~"}))))))

