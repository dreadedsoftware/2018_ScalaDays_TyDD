Here is a simple function
```tut:book
def zip(a: List[Int], b: List[Int]): List[(Int, Int)] = a.zip(b)
```
And this is how it is called
```tut:book
zip(1 :: 2 :: 3 :: Nil, 4 :: 5 :: 6 :: Nil)
```
This seems like a nice piece of well organized code. Let's try another.
```tut:book
def zip(a: List[Int], b: List[Int]): List[(Int, Int)] = b.zip(a)
```
And this is how it is called
```tut:book
zip(1 :: 2 :: 3 :: Nil, 4 :: 5 :: 6 :: Nil)
```
Now we have a predicament! Both of these functions are valid instantiations of the same signature `(List[Int], List[Int]) => List[(Int, Int)]` but mean very different things! This signature is insufficient.

We can refine this. The function body makes no use of any of the elements inside the lists. Maybe, the function can abstract this detail and remain unchanged.
```tut:book
def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
```
And the other way around?
```tut:book:fail
def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = b.zip(a)
```
That didn't quite work, we'll need to update our left hand side as well.
```tut:book
def zip[A, B](a: List[A], b: List[B]): List[(B, A)] = b.zip(a)
```
Now we are getting somewhere. When we abstract a type, we limit the implementation. When the implementation is sufficiently limited through abstraction a change in values implies a change in types.
Abstraction has helped guide us through the development process.

# Exercises
Using only values and functions (no classes, no traits)
1. Write a function which can zip 3 `List`s.
2. Write a function which can zip 3 `Vector`s.
