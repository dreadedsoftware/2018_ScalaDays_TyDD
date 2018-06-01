Here is a simple function
```scala
def zip(a: List[Int], b: List[Int]): List[(Int, Int)] = a.zip(b)
// zip: (a: List[Int], b: List[Int])List[(Int, Int)]
```
And this is how it is called
```scala
zip(1 :: 2 :: 3 :: Nil, 4 :: 5 :: 6 :: Nil)
// res0: List[(Int, Int)] = List((1,4), (2,5), (3,6))
```
This seems like a nice piece of well organized code. Let's try another.
```scala
def zip(a: List[Int], b: List[Int]): List[(Int, Int)] = b.zip(a)
// zip: (a: List[Int], b: List[Int])List[(Int, Int)]
```
And this is how it is called
```scala
zip(1 :: 2 :: 3 :: Nil, 4 :: 5 :: 6 :: Nil)
// res1: List[(Int, Int)] = List((4,1), (5,2), (6,3))
```
Now we have a predicament! Both of these functions are valid instantiations of the same signature `(List[Int], List[Int]) => List[(Int, Int)]` but mean very different things! This signature is insufficient.

We can refine this. The function body makes no use of any of the elements inside the lists. Maybe, the function can abstract this detail and remain unchanged.
```scala
def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
// zip: [A, B](a: List[A], b: List[B])List[(A, B)]
```
And the other way around?
```scala
def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = b.zip(a)
// <console>:12: error: type mismatch;
//  found   : List[(B, A)]
//  required: List[(A, B)]
//        def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = b.zip(a)
//                                                                   ^
```
That didn't quite work, we'll need to update our left hand side as well.
```scala
def zip[A, B](a: List[A], b: List[B]): List[(B, A)] = b.zip(a)
// zip: [A, B](a: List[A], b: List[B])List[(B, A)]
```
Now we are getting somewhere. When we abstract a type, we limit the implementation. When the implementation is sufficiently limited through abstraction a change in values implies a change in types.
Abstraction has helped guide us through the development process.

# Exercises
Using only values and functions (no classes, no traits)
1. Write a function which can zip 3 `List`s.
2. Write a function which can zip 3 `Vector`s.
