(ns gemini.extended
  (:require [gemini.core :refer [def-matching-env rule]]
            [clojure.string :as str]))

(defn prepare-expr
  [e]
  (let [pos (atom 0)]
    (for [w (str/split e #" ")]
      {:pos (swap! pos inc) :word w})))

(defn basic-find-likeness
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
  "Compares 2 expressions (e1 and e2) and returns a collection of maps. To compare those 2 expressions, they're
   splitted by words and after all words from e1 are compared to words of e2 by the matching functions lfs.
   An automatic matching function is added as first matching function: the equal function. This function checks if
   2 words are equal.

   The shortcut arg is a shortcut function whose purpose is to optimize the matching. A shortcut function must take
   2 arguments are words. If the shortcut fn returns true for 2 words then those words are not compared with the
   matching functions.

   Each value of lfs is a map containing the keys :func and :likeness. The value of :func is a function returned by
   the def-matching-env macro. The value of :likeness is the value will have the :likeness property in a returned map when
   2 words match.

   Each item of the returned map collections is the description of the likeness of 2 words when they matche.
   The description data are:
   :word1 Word of the expression e1 matched
   :word2 Word of the expression e2 matched
   :pos1 The position of the word1 in its expression
   :pos2 The position of the word2 in its expression
   :likeness The likeness of the matching (= should be reserved to the equal function)"
  [e1 e2 shortcut lfs]
  (let [lfs+equal (cons {:func = :likeness "="} lfs)]
    (loop [ws1 (prepare-expr e1)
           ws2 (prepare-expr e2)
           matching-fns lfs+equal
           result []]      
      (if (and (seq ws1) (seq matching-fns))
        (let [founds (result-matching ws1 ws2 shortcut (first matching-fns))]
          (recur (remove-found ws1 founds) ws2 (rest matching-fns) (into result founds)))
        
        result))))

(defn find-likeness-without-shortcut
  "Likes the find-likeness function except you don't want shortcut."
  [e1 e2 & lfs]
  (apply find-likeness e1 e2 (constantly false) lfs))

