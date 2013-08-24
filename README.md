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

```[gemini "0.2.1"]```

If you use Maven:

```
<dependency>
  <groupId>gemini</groupId>
  <artifactId>gemini</artifactId>
  <version>0.2.1</version>
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

   


## License

Copyright © 2013 ChriX

Distributed under the Eclipse Public License, the same as Clojure.
