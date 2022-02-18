# NetLogo Functional Programming Extension (fp)

## Introduction:

The NetLogo functional programming extension introduces several higher order procedures commonly found in functional programming languages.  

---

## Using:

First, make sure to include the functional programming extension in the model as such:

```
extensions[fp]
```

If your model already uses other extensions, then it already has an  `extensions`  line in it, so just add  `fp`  to the list.

For more information on using NetLogo extensions, see the  [Extensions Guide](https://ccl.northwestern.edu/netlogo/docs/extensions.html).

### When to use:

In general, anything you can do with the functional programming extension in NetLogo, could also be done using the default primitives in NetLogo. However,  using the functional programming extension would allow you to take advantage of some common higher order procedures commonly found in other functional programming languages that are not included in the default primitives in NetLogo.

---

## Demo Model:

 [Functional Programming Example](https://raw.githubusercontent.com/NetLogo/FP-Extension/main/FP%20Example.nlogo)

---

## Primitives:

[`fp:take`](###fp:take) [`fp:drop`](###fp:drop) [`fp:scan`](###fp:scan) [`fp:compose`](###fp:compose) [`fp:pipe`](###fp:pipe) [`fp:curry`](###fp:curry) [`fp:find-indices`](###fp:find-indices) [`fp:find`](###fp:find)[`fp:flatten`](###fp:flatten) [`fp:zip`](###fp:zip) [`fp:unzip`](###fp:unzip)

---
### fp:take

 __`fp:take `__  _`number list`_  

Accepts a positive integer _number_ and a _list_. Returns a list consisting of the first _number_ elements in _list_, or returns the entire _list_ if _number_ is more than the number of elements in the given list. If _number_ is not an integer, it is rounded to the closest integer.

##### Examples:
```
; Take the first 2 elements in the list ["a" "b" "c"] 
fp:take 2 ["a" "b" "c"] 
=> ["a" "b"]

; Take the first 5 elements in the list [0 1 ... 9]
fp:take 5 (range 10) 
=> [0 1 2 3 4]
```

---
### fp:drop

 __`fp:drop `__  _`number list`_  

Accepts a positive integer _number_ and a _list_. Returns a list consisting of all the elements of the list except the first _number_ elements, or returns an empty list if _number_ is more than the number of elements in the given list. If _number_ is not an integer, it is rounded to the closest integer.

##### Examples:
```
; Drop all but the last 2 elements in the list ["a" "b" "c"] 
fp:drop 2 ["a" "b" "c"] 
=> ["c"]

; Drop the first 5 elements in the list [0 1 ... 9]
fp:drop 5 (range 10) 
=> [5 6 7 8 9]
```

---

### fp:scan

 __`fp:scan`__ _`reporter list`_ 

Accepts a  binary reporter (arity 2) and a list as parameters and will use the _reporter_ to collapse the elements from _list_ to return a list consisting of the running total from each element. That is, the first two elements in _list_ are operated upon by the _reporter_ and the resultant of that operation becomes the first element of the list returned. This value is then used along with the next element in the _list_ to obtain the second element in the resulting list and so on until the entire _list_ is used.

##### Example:
```
; Applies the addition operation to the given list starting from the beginning and returns a list consisiting of all the resulting values 
fp:scan + [1 2 3 4 5] 
=> [1 3 6 10 15]

; Applies the given anonymous reporter to the given list starting from the beginning and returns a list consisiting of all the resulting values 
fp:scan [[a b] -> a] [1 2 3 4 5] 
=> [1 1 1 1 1]
```

---

 ### fp:compose
 
__`fp:compose`__ _`reporter1 reporter2`_

(__`fp:compose`__ _`reporter1 ...`_) 

Accepts a minimum of two reporters and returns a single reporter which combines the given reporters. When the resulting reporter is called, the last given reporter is evaluated and its result is passed as the argument to the previous reporter until the first reporter is evaluated, producing the output. Every given reporter except for the last one must be unary (accept only one argument).

##### Example:
```
; Combines the two given reporters. The resulting reporter first adds 5 to x and then divides it by 2.
let f fp:compose [x -> x / 2] [x -> x + 5]
(runresult f 3)
=> 4

; Combines the three given reporters. The resulting reporter first adds the given arguments, then takes the absolute value of the addition and multiplies it by 10.
let g (fp:compose [x -> x * 10] abs +)
(runresult g -3 2) 
=> 10
```

---

 ### fp:pipe
 
__`fp:pipe`__ _`reporter1 reporter2`_

(__`fp:pipe`__ _`reporter1 ...`_) 

This primitive is the same as the compose primitive, except the reporters are evaluated in reverse order. Just like compose, it accepts a minimum of two reporters and returns a single reporter which combines the given reporters. When the resulting reporter is called, the first given reporter is evaluated and its result is passed as the argument to the next reporter until the last reporter is evaluated, producing the output. Every given reporter except for the first one must be unary (accept only one argument).

##### Example:
```
; Combines the two given reporters. The resulting reporter first divides x by 2 and then adds 5.
let f fp:pipe [x -> x / 2] [x -> x + 5]
(runresult f 4)
=> 7

; Combines the three given reporters. The resulting reporter first adds the given arguments, then multiplies the result by 10 and finally takes the absolute value of the result.
let g (fp:pipe + [x -> x * 10] abs)
(runresult g -3 2) 
=> 10
```

---

### fp:curry

__`fp:curry`__ _`reporter value`_

(__`fp:curry`__ _`reporter value1 ...`_) 

Accepts a reporter and at least one value. Returns the reporter such that the given value (or values) is now fixed to the first  argument (or first n arguments) of the given reporter. This enables the user to reduce the number of arguments the given reporter accepts by fixing or partially applying the given values to the first arguments of the reporter. If the number of values given exceeds the number of arguments the reporter can take, only the first n values will be applied where n is the arity of the reporter.

##### Example:
```
; Defines a function g that takes three arguments. Use curry to define a new function f that takes two argemunts, where the first argument of g is fixed to 1. 
to-report g [x y z] 
  report x + y + z 
end
let f fp:curry g 1
(runresult f 2 3) 
=> 6

; Curry the given values to the anonymous reporter, but only the first value can be fixed because the reporter accepts only one value.
(runresult (fp:curry [x -> x + 1] 1 2)) 
=> 2
```
---

### fp:find-indices

 __`fp:find-indices`__ _`reporter list`_ 

Accepts a Boolean reporter and a list. Returns the indices of every element in the given list that reports _True_ for the given reporter. Returns an empty list if the _reporter_ doesn't report _True_ for any of the items in the given list. The reporter must be a Boolean reporter.

##### Example:
```
; Finds the indices of the elements in the given list that are equal to 2.
fp:find-indices [x -> x = 2] [1 2 1 2]
=> [1 3]

; Finds the indices of the elements in the given list that are alpha-numerically less than the symbol "b".
fp:find-indices [x -> x < "b"] ["b" "a" "b" "c" "a"] => [1 4]
```

---
### fp:find

__`fp:find`__ _`reporter list`_ 

Accepts a Boolean reporter and a list. Returns the first element of the given list that reports _True_ for the given reporter. Returns _False_ if the _reporter_ doesn't report _True_ for any of the items in the given list. The reporter must be a Boolean reporter.

##### Example:
```
; Find the first element in the given list that starts with a "t"
fp:find [ s -> first s = "t" ] ["hi" "there" "everyone"]
=> "there"

; Find the first element in the given list that is equla to 3. Since there are none, it returns False.
fp:find [x -> x = 3] [1 2 1 2] 
=> False
```

---
### fp:flatten

__`fp:flatten`__ _`list`_ 

Accepts a list and returns the given list such that none of the elements in the list are enclosed in lists. It flattens the given list until there are no lists in it.

##### Example:
```
fp:flatten [[1 2 3][4 5]] 
=> [1 2 3 4 5]

fp:flatten [[[1 [2] 3]][[4 [5]] [6]]] 
=> [1 2 3 4 5 6]
```

---
### fp:zip

__`fp:zip`__ _`list1 list2`_

(__`fp:zip`__ _`list1 ...`_)

Accepts a minimum of one list as its argument and returns a list of tuples where the _n_<sup>th</sup> item in each sublist of the given list are paired together with each other, creating a new list of tuples from the given lists. If the given lists are not the same length, the number of tuples returned will be the length of the shortest given list.

##### Example:
```
; Create a new list from the given two lists where the ith items are paired together
fp:zip [1 2 3] [4 5 6] 
=> [[1 4] [2 5] [3 6]]

; Create a new list from the given three lists where the ith items are collected together
(fp:zip [1 2] [3 4] [5 6])
=> [[1 3 5] [2 4 6]]
```

---

### fp:unzip

 __`fp:unzip`__ _`list`_

This is similar to zip, except it accepts a list of lists as its argument. Returns a list of tuples where the _n_<sup>th</sup> item the given list of lists are paired together with each other, creating a new list of tuples from the given list of lists. If the given list contains lists of different lengths, the length of the tuples in the returned list will be different accordingly.

##### Example:
```
; Create a new list from the given list of lists where the ith items in the sublists are paired together
fp:unzip [[1 4] [2 5] [3 6]] 
=> [[1 2 3] [4 5 6]]

; Create a new list from the given list of lists where the ith items in the sublists are paired together. In this case the sublists in the given list are of different lengths, so the sublists in the list that is returned are also of different lengths.
fp:unzip [[1 "a"] [2 "b"] [3 "c"] [4]] 
=> [[1 2 3 4] ["a" "b" "c"]]
```

---


