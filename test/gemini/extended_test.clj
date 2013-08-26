(ns gemini.extended-test
  (:require [clojure.test :refer :all]
            [gemini.extended :refer :all]
            [gemini.core :refer [def-matching-env rule]]))

(deftest test-find-likeness
  (testing "should find likeness of 2 expressions with the low-level function"
    (let [strong? (def-matching-env 0
                    (rule :min-length 5 :max-errors 1))
          weak? (def-matching-env 0
                  (rule :min-length 4 :max-length 5 :max-errors 1)
                  (rule :min-length 6 :max-errors 2 :authorized {:all 1}))
          poor? (def-matching-env 1
                  (rule :max-length 3))

          ;; we don't compare words if they don't start with the same letter
          shortcut-fn (fn [a b] (not= (first a) (first b)))

          strong-lk {:func strong? :likeness "S"}
          weak-lk {:func weak? :likeness "W"}
          poor-lk {:func poor? :likeness "P"}]

      (is (= [{:likeness "=" :word1 "pipper" :word2 "pipper" :pos1 5 :pos2 5}
              {:likeness "W" :word1 "rose" :word2 "roze" :pos1 1  :pos2 2}
              {:likeness "W" :word1 "david" :word2 "davi" :pos1 2 :pos2 3}
              {:likeness "P" :word1 "who" :word2 "woh" :pos1 3, :pos2 4}]
             (find-likeness "rose david who daleks pipper" "dalisk roze davi woh pipper" shortcut-fn strong-lk weak-lk poor-lk)))))

  (testing "should find likeness of 2 expression with the configured function"
    (let [strong? (def-matching-env 0
                    (rule :min-length 5 :max-errors 1))
          weak? (def-matching-env 0
                  (rule :min-length 4 :max-length 5 :max-errors 1)
                  (rule :min-length 6 :max-errors 2 :authorized {:all 1}))
          poor? (def-matching-env 1
                  (rule :max-length 3))

          ;; we don't compare words if they don't start with the same letter
          shortcut-fn (fn [a b] (not= (first a) (first b)))

          strong-lk {:func strong? :likeness "S"}
          weak-lk {:func weak? :likeness "W"}
          poor-lk {:func poor? :likeness "P"}

          likeness (config-find-likeness shortcut-fn strong-lk weak-lk poor-lk)]

      (is (= [{:likeness "=" :word1 "pipper" :word2 "pipper" :pos1 5 :pos2 5}
              {:likeness "W" :word1 "rose" :word2 "roze" :pos1 1  :pos2 2}
              {:likeness "W" :word1 "david" :word2 "davi" :pos1 2 :pos2 3}
              {:likeness "P" :word1 "who" :word2 "woh" :pos1 3, :pos2 4}]
             (likeness "rose david who daleks pipper" "dalisk roze davi woh pipper")))))

  (testing "should find likeness of 2 expression by the macro"
    (with-likeness
      (def-likeness "S" (def-matching-env 0 (rule :min-length 5 :max-errors 1)))
      
      (def-likeness "W" (def-matching-env 0
                          (rule :min-length 4 :max-length 5 :max-errors 1)
                          (rule :min-length 6 :max-errors 2 :authorized {:all 1})))
      
      (def-likeness "P" (def-matching-env 1 (rule :max-length 3)))
      
      (def-shortcut (fn [a b] (not= (first a) (first b))))

      (is (= [{:likeness "=" :word1 "pipper" :word2 "pipper" :pos1 5 :pos2 5}
              {:likeness "W" :word1 "rose" :word2 "roze" :pos1 1  :pos2 2}
              {:likeness "W" :word1 "david" :word2 "davi" :pos1 2 :pos2 3}
              {:likeness "P" :word1 "who" :word2 "woh" :pos1 3, :pos2 4}]
             (search-likeness "rose david who daleks pipper" "dalisk roze davi woh pipper"))))))


