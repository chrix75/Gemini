(ns gemini.extended
  (:require [gemini.core :refer [def-matching-env rule]]
            [clojure.string :as str]))

(defn match-exprs
  [e1 e2 match-fn]
  (when (and (seq e1) (seq e2))
    (filter #(not (nil? %))
            (for [w1 (str/split e1 #" ") w2 (str/split e2 #" ")]
              (if (= w1 w2)
                {:w1 w1 :w2 w2 :likeness "="}
                (if (and (= (first w1) (first w2)) (match-fn w1 w2))
                  {:w1 w1 :w2 w2 :likeness "~"}))))))

