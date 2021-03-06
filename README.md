# Gemini

A Clojure library designed to make easy the data matching.

The data matching is a way to find 2 data are the same even if there're some differences. For example, when you compare two names like "SMITH" and "SMIHT", in some cases you can say the second name is the same as the first one with a keyboard input error.

In the big-data area, where information is what worth the most, the data matching is a good way to improve your data.

>Note: The levenshtein distance is a kind of function for data matching. But the purpose of the Gemini project is to yield more fine-tuned functions.

## Rationale

When your work is to process data all the day to find duplicate in databases, you need tools to improve the correctness of your results. 

For that, some rules must be applied following the data context. Indeed, compare 2 names and 2 account numbers shouldn't follow the same rules. A libray has been needed to define that rules… The idea of Gemini arose.

## Usage

### Dependency

If you use Leiningen then add this dependecy in your project.clj file:

```[gemini "0.3.2"]```

If you use Maven:

```
<dependency>
  <groupId>gemini</groupId>
  <artifactId>gemini</artifactId>
  <version>0.3.1</version>
</dependency>
``` 

After, you may include the library in your namespace declaration like that:

```clojure
(:require '[gemini.core :as gemini])
```

### Basics

The matching rules are defined in environments. That lets you define different cases like (for example):

* Rules about minor errors (you can consider 2 data with this sort of error are the same)
* Rules to errors that might lead to human validation
* Rules about weak likeness

To define an environment, you should use the ```def-matching-env``` macro. This macro returns a matching function will use the environment rules. A matching function takes 2 arguments are strings to compare and returns a boolean that says if the 2 datas are the _same_ following the rules.

An example (comes from the test code):

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

The ```def-matching-env``` macro takes as first argument the default max number of errors (see the rule definition for more information). After this number, you declare your rules.

### Rules ordering

The order of the rules declaration is important because the matching function uses only one rule by comparison.
The selected rule is the first one whose the selectors are validated.

### Rule definition

A rule is defined by the function ```rule``` inside the body of the ```def-matching-env``` macro.
The arguments of that function responds to key/values pattern. 

There are 2 sorts of keys:

* selectors
* validators

A selector defines when a rule is applied for the comparaison of 2 strings.

A validator defines how 2 data are declared as the same.

#### selectors

```:length n```
The rule is applied when the 2 tested strings have _n_ characters.

```:max-length n```
The rule is applied when the lengths of the 2 tested strings are _n_ characters at most.

```:min-length n```
The rule is applied when the lengths of the 2 tested strings are _n_ characters at least.

> If none selector is given then the rule may be applied regardless of the string length.
>
> You can define a gap by using the ```min-length``` and ```max-length``` in the same rule.


#### validators

A validator uses the type of found errors to say if 2 data may be the same or not. The known errors are codified by keywords:

* ```:inv``` An inversion is found ("AB" vs. "BA")
* ```:sub``` A substitution is found ("AB" vs. "CB") 
* ```:delete``` A deletion is found ("ABC" vs. "AC")
* ```:insert``` An insertion is found ("ABCD" vs. "ABC")

The underneath typed error keywords are used to declare validators.

The known validators are:

```:max-errors n```
The _n_ errors are accepted by the rule. If this validator is not provided then the rule uses the default value given in the ```def-matching-env``` macro.

```:authorized m``` 
In the map _m_, you define the number of errors grouped by their type the rule accepts.

> If you want to define all error types with the same max number of errors, you can code
> ```:autorized {:all 1}```.

```:forbidden v```
The vector _v_ contains a list of errors mustn't be found while the comparison of 2 strings.

> When a case in a validator is encountered then it overrides all others.
> Thus, if :max-errors is set to 3 and :forbidden set to [:sub] then the rule invalidates any comparison when a subsitution is found, even if the substitution is the first error.

### The default rule

When you define a matching environment with the ```def-matching-env``` macro, a default rule is added as last one. That rule has neither selector nor validator and it's here to invalidate a comparison when the number of found errors exceeds the default max-errors value set in the macro call.

## Use cases

### Account numbers

You have many account numbers in a file are not found in your customers databases. Perhaps, some of them are valid account numbers with one inversion. Often, operators make this error when they type an account number by the numeric keyboard pad.

To solve this issue, you define this matching environment:

```clojure
(let [valid-account? (def-matching-env 1
                              (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))]
…)                              
```

### Several environments

You take our account number case but this time, we want to manage 2 cases:

1. The account numbers are valid despite an error (the inversion)
2. An account may be a valid account if we have a substitution

For that case, we define 2 environments. The account numbers that don't pass the first environment will pass the second one.

```clojure
(let [valid-account? (def-matching-env 1
                              (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))]
…)

(let [maybe-account? (def-matching-env 1
                              (rule :authorized {:sub 1} :forbidden [:inv :insert :delete]))]

…)
```

### A french case

> Thanks to Bastien for that case.

Let's imagine you have french data values (with accentued characters).
First, you have a collection: ```"église" "Eglise" "Église" "Elise" "Élise" "élise"``` and you have as input value ```Égilse```.

You expect to have this result ```"église" "Eglise" "Église"```.

Below the code:

```clojure
;; we define a function to clean and prepare the compared value
(defn clean-value [s] (-> (.toUpperCase s) (clojure.string/replace #"[ÉÈÊË]" "E")))

;; It's the input data
(def v (clean-value "Égilse"))

;; Here, the rule 
(def my-f (def-matching-env 1 (rule :authorized {:inv 1} :forbidden [:sub :delete :insert])))

;; And now, we search the values from the collection
(filter #(my-f v (clean-value %)) coll)
```
**Updated for the version 0.2.0**

```clojure
(let [coll ["église" "Eglise" "Église" "Elise" "Elise" "élise"]
          clean-fn (fn [s] (-> (clojure.string/upper-case s) (clojure.string/replace #"[ÉÈÊË]" "E")))
          found? (def-matching-env 1 (rule :authorized {:inv 1} :forbidden [:sub :insert :delete]))
          fuzzy-filter (fuzzy-filter-fn found? clean-fn clean-fn)]

      (is (= ["église" "Eglise" "Église"] (fuzzy-filter coll "Égilse"))))
```

## Helper functions

### fuzzy-filter-fn

> See the french use case

Returns a function that filters collection by using a matching function.
   
The first argument of the fuzzy-filter-fn is the matching function returned by the def-matching-env macro.

The fns arguments are (in the order): the cleansing function for the input data and the next one is to clean the collection items.

The returned function take 2 args: the collection and the input data.      

>If you clean only the collection item, you pass identity function as the input data cleansing function.

>The cleansing functions must take one argument: the data to clean.

## Extended functions

The extended functions are functions rest upon the Gemini core and provide useful results while expressions comparaisons.

### find-likeness

The purpose of this function is to find the likeness between 2 expressions. For this function, an expression is several words separated by one space.

The ```find-likeness``` function find the likeness thanks to matching functions defined by the ```def-matching-env``` macro. 
You link each of those matching functions with a likeness marker in a map (you'll see an example below).

The function use a shortcut function too. This function lets you don't test 2 words while likeness search. For instance, you can define a shortcut function to avoid compare 2 words when they don't start with the same letter.

Below, an example from the tests:

```clojure
(deftest test-find-likeness
  (testing "should find likeness of 2 expressions"
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
             (find-likeness "rose david who daleks pipper" "dalisk roze davi woh pipper" shortcut-fn strong-lk weak-lk poor-lk))))))

```

In this example, we define 3 matching functions to mark different kinds of likeness and a shorcut-function (the one described before.)

In the test clause, we see the result of the ```find-likeness``` function. 

> NOTE: As you see, the ```find-likeness``` function defines by itself an equal matching function. This function is put before all other matching function you provide.

After, according to your domain and business rules, you can decide if the 2 expressions characterize the same data.


### find-likeness-without-shortcut

I think the name is clear enough :)
Its use is like the ```find-likeness``` except you don't define and give a shortcut function.

### config-find-likeness

This function returns a configured function (with likeness and shortcut if any). Thus, you can search the likeness between  expressions without to provide for each call all the configuration.
(see the extended tests)

### with-likeness

This macro reduces the quantity of code and makes the life of the developer easier.

Take a look:

```clojure
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
             (search-likeness "rose david who daleks pipper" "dalisk roze davi woh pipper")))))
```

In this macro, you have the function ```def-likeness```, ```def-shortcut``` and ```search-likeness```.
I think the names and the test above give information enough :)

> Don't forget you don't have to define a shorcut function if you don't want.
>
> IMPORTANT: The first version of this macro (v. 0.3.1) always returned nil. From the Gemini version 0.3.2, 
> this macro doesn't return nil.


## License

Copyright © 2013 ChriX

Distributed under the Eclipse Public License, the same as Clojure.
