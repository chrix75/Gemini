# Gemini

A Clojure library designed to make easy data matching.

The data matching is a way to find 2 data are the same even if there're some differences. For example, when you compare two names like "SMITH" and "SMIHT", in some cases you can say the second name is the same as the first one with a keyboard input error.

In the big-data area, where information is what worth the most, the data matching is a good way to improve your data.

>Note: The levenshtein distance is a kind of function for data matching. But the purpose of the Gemini project is to yield more fine-tuned functions.

## Rationale

When your work is to process data all the day to find duplicate in databases, you need tools to improve the correctness of your results. 

For that, some rules must be applied following the data context. Indeed, compare 2 names and 2 account numbers shouldn't follow the same rules. A libray has been needed to define that rules… The idea of Gemini arose.

## Usage

FIXME

## License

Copyright © 2013 ChriX

Distributed under the Eclipse Public License, the same as Clojure.
