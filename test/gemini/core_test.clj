(ns gemini.core-test
  (:require [clojure.test :refer :all]
            [gemini.core :refer :all]))

(deftest test-find-errors
  (testing "should find no error"
    (let [noerror (find-errors "abc" "abc")]
      (is (= 0 (count noerror)))))
  
  (testing "should find a deletion in the middle"
    (let [error (find-errors "abc" "ac")]
      (is (= 1 (count error)))
      (is (= :delete (:type (first error))))))

  (testing "should find a deletion at the beginning"
    (let [error (find-errors "abc" "bc")]
      (is (= 1 (count error)))
      (is (= :delete (:type (first error))))))

  (testing "should find a deletion at the end"
    (let [error (find-errors "ab" "abc")]
      (is (= 1 (count error)))
      (is (= :delete (:type (first error))))))

  (testing "should find a substitution and and inversion"
    (let [errors (find-errors "abcd" "ebdc")]
      (is (= 2 (count errors)))
      (is (= :sub (:type (first errors))))
      (is (= :inv (:type (second errors)))))))

(deftest test-rules
  (testing "should define a matching environment with rules"
    (let [env (-> (matching-env 2)
                  (rule :max-length 4 :authorized {:inv 1 :delete 2} :max-errors 3 :forbidden [:sub])
                  (rule :length 5 :authorized {:sub 1}))]

      (is (= 2 (count (:rules env))))
      (is (= 2 (:max-errors (second (:rules env)))))))

  ;; In this test, we define 3 rules. The last one is a special rule.
  ;; It means: For all words and use the environment default
  ;; max-errors value.
  (testing "should return the accepted rule"
    (let [env (-> (matching-env 2)
                  (rule :max-length 4 :authorized {:inv 1 :delete 2} :max-errors 3 :forbidden [:sub])
                  (rule :length 5 :authorized {:sub 1})
                  (rule))]

      (is (= 5 (:length (accepted-rule (:rules env) "abcde" "abcde"))))
      (is (= 4 (:max-length (accepted-rule (:rules env) "abc" "abcd"))))))

  ;; In the following test, we define a matching env with 2 rules.
  ;; The first rule concerns the words whose length is between 1 and 4
  ;; characters.
  ;; The second rule if for the words with a 5 characters length.  
  (testing "should validate candidates using rules"
    (let [env (-> (matching-env 2)
                  (rule :max-length 4 :authorized {:inv 1 :delete 2} :forbidden [:sub])
                  (rule :length 5 :authorized {:sub 1} :max-errors 3))
          ruled-candidates? (ruled-candidates-fn env)]

      (is (false? (ruled-candidates? "foo" "bar")))
      (is (true? (ruled-candidates? "foo" "foo")))
      (is (true? (ruled-candidates? "bar" "bra")))))

  (testing "should validate candidates using rules by defining the matching env with the macro"
    (let [ruled-candidates? (def-matching-env 2
                              (rule :max-length 4 :authorized {:inv 1 :delete 2} :forbidden [:sub])
                              (rule :length 5 :authorized {:sub 1} :max-errors 3))]
      
      (is (false? (ruled-candidates? "foo" "bar")))
      (is (true? (ruled-candidates? "foo" "foo")))
      (is (true? (ruled-candidates? "bar" "bra")))

      ;; the 2 tests below use the default rule
      (is (true? (ruled-candidates? "123456789" "123456789")))
      (is (false? (ruled-candidates? "123456789" "132457698"))))))

(deftest test-search-coll
  ;; in the test below only one inversion error is authorized
  (testing "should return the items from a collection match with rules (with cleansing function for all data)"
    (let [s "Doctor Woh"
          coll ["DOCTOR WHO" "doctor who" "dalek" "DALEK"]
          found? (def-matching-env 1 (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))
          fuzzy-filter (fuzzy-filter-fn found? clojure.string/upper-case clojure.string/upper-case)]

      (is (= ["DOCTOR WHO" "doctor who"] (fuzzy-filter coll s)))))

  (testing "should return the item from a collection match with rules (in this test, the collection items are clean already)"
    (let [s "Doctor Woh"
          coll ["DOCTOR WHO" "doctor who" "dalek" "DALEK"]
          found? (def-matching-env 1 (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))
          ;; no cleansing function for the collection items
          fuzzy-filter (fuzzy-filter-fn found? clojure.string/upper-case)]

      (is (= ["DOCTOR WHO"] (fuzzy-filter coll s)))))

  (testing "shouldn't return any item from a collection because no cleansing functions are provided"
    (let [s "Doctor Woh"
          coll ["DOCTOR WHO" "doctor who" "dalek" "DALEK"]
          found? (def-matching-env 1 (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))
          ;; no cleansing function for the collection items
          fuzzy-filter (fuzzy-filter-fn found?)]

      (is (empty? (fuzzy-filter coll s)))))

  (testing "A french case"
    (let [s "Égilse"
          coll ["église" "Eglise" "Église" "Elise" "Elise" "élise"]
          clean-fn (fn [s] (-> (clojure.string/upper-case s) (clojure.string/replace #"[ÉÈÊË]" "E")))
          found? (def-matching-env 1 (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))
          fuzzy-filter (fuzzy-filter-fn found? clean-fn clean-fn)]

      (is (= ["église" "Eglise" "Église"] (fuzzy-filter coll s))))))
