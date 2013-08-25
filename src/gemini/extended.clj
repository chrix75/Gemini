(ns gemini.extended
  (:require [gemini.core :refer [def-matching-env rule]]
            [clojure.string :as str]))

(defn prepare-expr
  [e]
  (let [pos (atom 0)]
    (for [w (str/split e #" ")]
      {:pos (swap! pos inc) :word w})))

(defn analyze-expressions-likeness
  "Takes 2 expressions and calls the matching function matching function f on expressions words.
   Each word of the expression e1 is compared to every word of the expression e2.

   This function returns a lazy sequence of maps. Each item corresponds to a matching word.
   The map has the following data:
     :word1 > The word of the expression e1
     :word2 > The word of the expression e2
     :pos1 > The position of word1 in e1
     :pos2 > The position of word2 in e2
     :likeness > The level of words likeness. If likeness is = then there is no error,
                 if likeness is ~ then errors were found."
  [e1 e2 f]
  (when (and (seq e1) (seq e2))
    (filter #(not (nil? %))
            (for [d1 (prepare-expr e1) d2 (prepare-expr e2)]
              (let [w1 (:word d1) w2 (:word d2)
                    p1 (:pos d1) p2 (:pos d2)
                    r {:word1 w1 :word2 w2 :pos1 p1 :pos2 p2}]
                   
                (if (= w1 w2)
                  (assoc r :likeness "=")
                  (if (and (= (first w1) (first w2)) (f w1 w2))
                    (assoc r :likeness "~"))))))))


(defn result-matching
  [ws1 ws2 sf {:keys [func likeness]}]
  (when (and (seq ws1) (seq ws2))
    (filter #(not (nil? %)) (for [w1 ws1 w2 ws2 :when (not (sf w1 w2))]
                              (when (func (:word w1) (:word w2))
                                  {:likeness likeness
                                   :word1 (:word w1) :word2 (:word w2)
                                   :pos1 (:pos w1) :pos2 (:pos w2)})
                              ))))

(defn remove-found
  [ws founds]
  (let [pos2remove (for [w ws f founds :when (= (:pos w) (:pos1 f))] (:pos w))]
    (reduce (fn [retaineds {:keys [pos] :as w}]
              (if-not (some #(= pos %) pos2remove)
                (conj retaineds w)
                retaineds)) [] ws)))

(defn find-likeness
  [e1 e2 shortcut f & lfs]
  (let [lfs+equal (cons {:func = :likeness "="} (cons f lfs))]
    (loop [ws1 (prepare-expr e1)
           ws2 (prepare-expr e2)
           matching-fns lfs+equal
           result []]      
      (if (and (seq ws1) (seq matching-fns))
        (let [founds (result-matching ws1 ws2 shortcut (first matching-fns))]
          (recur (remove-found ws1 founds) ws2 (rest matching-fns) (into result founds)))
        
        result))))

(defn find-likeness-without-shortcut
  [e1 e2 f & lfs]
  (apply find-likeness e1 e2 (constantly false) f lfs))

