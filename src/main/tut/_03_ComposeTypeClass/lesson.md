#Previous Exercises
## Write a type class which is *both* A and B.
```tut:book
trait And[A, B]{
  def left: A
  def right: B
}
```

## Write a type class which is only A or only B. 
```tut:book
trait Or[A, B]{
  def left: Option[A]
  def right: Option[B]
}
```
*Note: A further refinement is left as a point of contemplation.*

# Implicits
The `implicit` keyword is one of the most useful (and confusing) constructs in Scala. It allows the developer to imply requirements without listing them in turn. We can employ it thus

Recall `Zip` and `zip`.
```tut:book
trait Zip[F[_]]{
  def zip[A, B](a: F[A], b: F[B]): F[(A, B)]
}

def zip[F[_], A, B](a: F[A], b: F[B], F: Zip[F]): F[(A, B)] =
  F.zip(a, b)
```
Here is a `Zip` instance for List
```tut:book
def zipList: Zip[List] = new Zip[List]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
```
And this is how we use these together
```tut:book
zip(List('a', 'b', 'c'), List(1, 2, 3), zipList)
```
It is clear a `Zip[List]` is required at the call site, it seems unreasonable to pass it each time we need to call the function especially when building larger zip calls
```tut:book
def zip3[F[_], A, B, C](a: F[A], b: F[B], c: F[C], F: Zip[F]): F[(A, (B, C))] =
  zip(a, zip(b, c, F), F)
def zip4[F[_], A, B, C, D](a: F[A], b: F[B], c: F[C], d: F[D], F: Zip[F]): F[(A, (B, (C, D)))] =
  zip(a, zip(b, zip(c, d, F), F), F)
def zip5[F[_], A, B, C, D, E](a: F[A], b: F[B], c: F[C], d: F[D], e: F[E], F: Zip[F]): F[(A, (B, (C, (D, E))))] =
  zip(a, zip(b, zip(c, zip(d, e, F), F), F), F)
```
This is more cumbersome than it seems necessary. Through the `implicit` keyword, Scala provides a mechanism for threading type class instances through call stacks
```tut:book
implicit def zip[F[_], A, B](implicit a: F[A], b: F[B], F: Zip[F]): F[(A, B)] =
  F.zip(a, b)
implicit def zipList: Zip[List] = new Zip[List]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}

def zip3[F[_], A, B, C](implicit a: F[A], b: F[B], c: F[C], F: Zip[F]): F[(A, (B, C))] =
  implicitly
def zip4[F[_], A, B, C, D](implicit a: F[A], b: F[B], c: F[C], d: F[D], F: Zip[F]): F[(A, (B, (C, D)))] =
  implicitly
def zip5[F[_], A, B, C, D, E](implicit a: F[A], b: F[B], c: F[C], d: F[D], e: F[E], F: Zip[F]): F[(A, (B, (C, (D, E))))] =
  implicitly
```
Or even with a shorthand
```tut:book
def zip3[F[_]: Zip, A: F, B: F, C: F]: F[(A, (B, C))] =
  implicitly
def zip4[F[_]: Zip, A: F, B: F, C: F, D: F]: F[(A, (B, (C, D)))] =
  implicitly
def zip5[F[_]: Zip, A: F, B: F, C: F, D: F, E: F]: F[(A, (B, (C, (D, E))))] =
  implicitly
```
Here we can see similar syntax moves can be made with our type class, `Zip`, instance and our `List` instances. `List` is a type class (the Free Monoid).

# Going Further
Our `Zip` type class seems really awesome. It can compose arbitrarily long instances of nested `Tuple2`s. But what about not using `Tuple2`? What if we want to use our `And` type or our `Or` type? Let's abstract this detail from the definition.

## What is it we are trying to abstract exactly?
We have a `Tuple2` requirement which is a detail the `Zip` type class does not require for its definition. Since the detail is unused, it can be forgotten. We care about three types: `Tuple2[A, B]`, `And[A, B]`, `Or[A, B]`. They all take two proper type parameters. Let's try to replace this by a suitable variable.

```tut:book
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
```
Just like we can abstract over types which have a single type parameter, we can do the same with multiple type parameters! And here are our instances.
```tut:book
implicit def zipListTuple2: Zip[List, Tuple2] = new Zip[List, Tuple2]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
implicit def zipListAnd: Zip[List, And] = new Zip[List, And]{
  override def zip[A, B](a: List[A], b: List[B]): List[And[A, B]] = {
    val size = Math.min(a.size, b.size)
    (0 until size).toList.map{idx =>
      new And[A, B]{
        def left: A = a(idx)
        def right: B = b(idx)
      }
    }
  }
}
implicit def zipListOr: Zip[List, Or] = new Zip[List, Or]{
  override def zip[A, B](a: List[A], b: List[B]): List[Or[A, B]] = {
    val size = Math.max(a.size, b.size)
    (0 until size).toList.map{idx =>
      new Or[A, B]{
        def left: Option[A] = if(a.size < idx) Some(a(idx)) else None
        def right: Option[B] = if(b.size < idx) Some(b(idx)) else None
      }
    }
  }
}
implicit def a: List[Int] = List(1)
implicit def b: List[String] = List("2")
implicit def c: List[Double] = List(3.0)
implicit def d: List[Long] = List(4L)
implicit def e: List[Char] = List('5')
```
*The next part has a caveat. Always put "more complicated" type classes first, there is a `WONT_FIX` bug in the compiler we need to work around.*
```tut:book
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)

def zip3[F[_], G[_, _], A: F, B: F, C: F](implicit F: Zip[F, G]): F[G[A, G[B, C]]] =
  implicitly
def zip5[F[_], G[_, _], A: F, B: F, C: F, D: F, E: F](implicit F: Zip[F, G]): F[G[A, G[B, G[C, G[D, E]]]]] =
  implicitly
```
Now, the business logic for `Tuple2`
```tut:book
zip5[List, Tuple2, Int, String, Double, Long, Char]
zip5[List, Tuple2, Int, String, Char, Long, Double]
zip5[List, Tuple2, String, Int, Double, Long, Char]
```
for `And`
```tut:book
zip5[List, And, Int, String, Double, Long, Char]
```
And finally `Or`
```tut:book
zip5[List, Or, Int, String, Double, Long, Char]
```
The business logic need not change. All we need to do is make sure all of our type class instances are in scope and we can rearrange and redeclare at will. Type Driven Development is the most declarative discipline I have found; declare a bunch of types and instances, the compiler does the rest.

#Exercises
1. Define The Non-Empty Binary Tree

