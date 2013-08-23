# Gemini

A Clojure library designed to make easy data matching.

The data matching is a way to find 2 data are the same even if there're some differences. For example, when you compare two names like "SMITH" and "SMIHT", in some cases you can say the second name is the same as the first one with a keyboard input error.

In the big-data area, where information is what worth the most, the data matching is a good way to improve your data.

>Note: The levenshtein distance is a kind of function for data matching. But the purpose of the Gemini project is to yield more fine-tuned functions.

## Rationale

When your work is to process data all the day to find duplicate in databases, you need tools to improve the correctness of your results. 

For that, some rules must be applied following the data context. Indeed, compare 2 names and 2 account numbers shouldn't follow the same rules. A libray has been needed to define that rules… The idea of Gemini arose.

## Usage

### Basics

The matching rules are defined in environments. That lets you define different cases like (for example):

* Rules about minor errors (you can consider 2 data with this sort of error are the same)
* Rules to errors that might lead to human validation
* Rules about weak likeness

To define an environment, you should use the ```def-matching-env``` macro. This macro returns a matching function will use the environment rules. A matching function takes 2 arguments are strings to compare and returns a boolean that says if the 2 datas are the _same_ following the rules.

An example (can be found in the test code):

```clojure
(let [ruled-candidates? (def-matching-env 2
                              (rule :max-length 4 :authorized {:inv 1 :delete 2} :forbidden [:sub])
                              (rule :length 5 :authorized {:sub 1} :max-errors 3))]
      
      (is (false? (ruled-candidates? "foo" "bar")))
      (is (true? (ruled-candidates? "foo" "foo")))
      (is (true? (ruled-candidates? "bar" "bra")))

      ;; the 2 tests below use the default rule
      (is (true? (ruled-candidates? "123456789" "123456789")))
      (is (false? (ruled-candidates? "123456789" "132457698"))))
```

### Rule definition

TODO

### The default rule

TODO

## License

Copyright © 2013 ChriX

Distributed under the Eclipse Public License, the same as Clojure.
