#Previous Exercises
## Write a type class which is *both* A and B.
```scala
trait And[A, B]{
  def left: A
  def right: B
}
// defined trait And
```

## Write a type class which is only A or only B. 
```scala
trait Or[A, B]{
  def left: Option[A]
  def right: Option[B]
}
// defined trait Or
```
*Note: A further refinement is left as a point of contemplation.*

# Implicits
The `implicit` keyword is one of the most useful (and confusing) constructs in Scala. It allows the developer to imply requirements without listing them in turn. We can employ it thus

Recall `Zip` and `zip`.
```scala
trait Zip[F[_]]{
  def zip[A, B](a: F[A], b: F[B]): F[(A, B)]
}
// defined trait Zip

def zip[F[_], A, B](a: F[A], b: F[B], F: Zip[F]): F[(A, B)] =
  F.zip(a, b)
// zip: [F[_], A, B](a: F[A], b: F[B], F: Zip[F])F[(A, B)]
```
Here is a `Zip` instance for List
```scala
def zipList: Zip[List] = new Zip[List]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
// zipList: Zip[List]
```
And this is how we use these together
```scala
zip(List('a', 'b', 'c'), List(1, 2, 3), zipList)
// res0: List[(Char, Int)] = List((a,1), (b,2), (c,3))
```
It is clear a `Zip[List]` is required at the call site, it seems unreasonable to pass it each time we need to call the function especially when building larger zip calls
```scala
def zip3[F[_], A, B, C](a: F[A], b: F[B], c: F[C], F: Zip[F]): F[(A, (B, C))] =
  zip(a, zip(b, c, F), F)
// zip3: [F[_], A, B, C](a: F[A], b: F[B], c: F[C], F: Zip[F])F[(A, (B, C))]

def zip4[F[_], A, B, C, D](a: F[A], b: F[B], c: F[C], d: F[D], F: Zip[F]): F[(A, (B, (C, D)))] =
  zip(a, zip(b, zip(c, d, F), F), F)
// zip4: [F[_], A, B, C, D](a: F[A], b: F[B], c: F[C], d: F[D], F: Zip[F])F[(A, (B, (C, D)))]

def zip5[F[_], A, B, C, D, E](a: F[A], b: F[B], c: F[C], d: F[D], e: F[E], F: Zip[F]): F[(A, (B, (C, (D, E))))] =
  zip(a, zip(b, zip(c, zip(d, e, F), F), F), F)
// zip5: [F[_], A, B, C, D, E](a: F[A], b: F[B], c: F[C], d: F[D], e: F[E], F: Zip[F])F[(A, (B, (C, (D, E))))]
```
This is more cumbersome than it seems necessary. Through the `implicit` keyword, Scala provides a mechanism for threading type class instances through call stacks
```scala
implicit def zip[F[_], A, B](implicit a: F[A], b: F[B], F: Zip[F]): F[(A, B)] =
  F.zip(a, b)
// zip: [F[_], A, B](implicit a: F[A], implicit b: F[B], implicit F: Zip[F])F[(A, B)]

implicit def zipList: Zip[List] = new Zip[List]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
// zipList: Zip[List]

def zip3[F[_], A, B, C](implicit a: F[A], b: F[B], c: F[C], F: Zip[F]): F[(A, (B, C))] =
  implicitly
// zip3: [F[_], A, B, C](implicit a: F[A], implicit b: F[B], implicit c: F[C], implicit F: Zip[F])F[(A, (B, C))]

def zip4[F[_], A, B, C, D](implicit a: F[A], b: F[B], c: F[C], d: F[D], F: Zip[F]): F[(A, (B, (C, D)))] =
  implicitly
// zip4: [F[_], A, B, C, D](implicit a: F[A], implicit b: F[B], implicit c: F[C], implicit d: F[D], implicit F: Zip[F])F[(A, (B, (C, D)))]

def zip5[F[_], A, B, C, D, E](implicit a: F[A], b: F[B], c: F[C], d: F[D], e: F[E], F: Zip[F]): F[(A, (B, (C, (D, E))))] =
  implicitly
// zip5: [F[_], A, B, C, D, E](implicit a: F[A], implicit b: F[B], implicit c: F[C], implicit d: F[D], implicit e: F[E], implicit F: Zip[F])F[(A, (B, (C, (D, E))))]
```
Or even with a shorthand
```scala
def zip3[F[_]: Zip, A: F, B: F, C: F]: F[(A, (B, C))] =
  implicitly
// zip3: [F[_], A, B, C](implicit evidence$1: Zip[F], implicit evidence$2: F[A], implicit evidence$3: F[B], implicit evidence$4: F[C])F[(A, (B, C))]

def zip4[F[_]: Zip, A: F, B: F, C: F, D: F]: F[(A, (B, (C, D)))] =
  implicitly
// zip4: [F[_], A, B, C, D](implicit evidence$1: Zip[F], implicit evidence$2: F[A], implicit evidence$3: F[B], implicit evidence$4: F[C], implicit evidence$5: F[D])F[(A, (B, (C, D)))]

def zip5[F[_]: Zip, A: F, B: F, C: F, D: F, E: F]: F[(A, (B, (C, (D, E))))] =
  implicitly
// zip5: [F[_], A, B, C, D, E](implicit evidence$1: Zip[F], implicit evidence$2: F[A], implicit evidence$3: F[B], implicit evidence$4: F[C], implicit evidence$5: F[D], implicit evidence$6: F[E])F[(A, (B, (C, (D, E))))]
```
Here we can see similar syntax moves can be made with our type class, `Zip`, instance and our `List` instances. `List` is a type class (the Free Monoid).

# Going Further
Our `Zip` type class seems really awesome. It can compose arbitrarily long instances of nested `Tuple2`s. But what about not using `Tuple2`? What if we want to use our `And` type or our `Or` type? Let's abstract this detail from the definition.

## What is it we are trying to abstract exactly?
We have a `Tuple2` requirement which is a detail the `Zip` type class does not require for its definition. Since the detail is unused, it can be forgotten. We care about three types: `Tuple2[A, B]`, `And[A, B]`, `Or[A, B]`. They all take two proper type parameters. Let's try to replace this by a suitable variable.

```scala
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
// defined trait Zip
```
Just like we can abstract over types which have a single type parameter, we can do the same with multiple type parameters! And here are our instances.
```scala
implicit def zipListTuple2: Zip[List, Tuple2] = new Zip[List, Tuple2]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
// zipListTuple2: Zip[List,Tuple2]

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
// zipListAnd: Zip[List,And]

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
// zipListOr: Zip[List,Or]

implicit def a: List[Int] = List(1)
// a: List[Int]

implicit def b: List[String] = List("2")
// b: List[String]

implicit def c: List[Double] = List(3.0)
// c: List[Double]

implicit def d: List[Long] = List(4L)
// d: List[Long]

implicit def e: List[Char] = List('5')
// e: List[Char]
```
*The next part has a caveat. Always put "more complicated" type classes first, there is a `WONT_FIX` bug in the compiler we need to work around.*
```scala
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
// zip: [F[_], G[_, _], A, B](implicit F: Zip[F,G], implicit a: F[A], implicit b: F[B])F[G[A,B]]

def zip3[F[_], G[_, _], A: F, B: F, C: F](implicit F: Zip[F, G]): F[G[A, G[B, C]]] =
  implicitly
// zip3: [F[_], G[_, _], A, B, C](implicit evidence$1: F[A], implicit evidence$2: F[B], implicit evidence$3: F[C], implicit F: Zip[F,G])F[G[A,G[B,C]]]

def zip5[F[_], G[_, _], A: F, B: F, C: F, D: F, E: F](implicit F: Zip[F, G]): F[G[A, G[B, G[C, G[D, E]]]]] =
  implicitly
// zip5: [F[_], G[_, _], A, B, C, D, E](implicit evidence$1: F[A], implicit evidence$2: F[B], implicit evidence$3: F[C], implicit evidence$4: F[D], implicit evidence$5: F[E], implicit F: Zip[F,G])F[G[A,G[B,G[C,G[D,E]]]]]
```
Now, the business logic for `Tuple2`
```scala
zip5[List, Tuple2, Int, String, Double, Long, Char]
// res1: List[(Int, (String, (Double, (Long, Char))))] = List((1,(2,(3.0,(4,5)))))

zip5[List, Tuple2, Int, String, Char, Long, Double]
// res2: List[(Int, (String, (Char, (Long, Double))))] = List((1,(2,(5,(4,3.0)))))

zip5[List, Tuple2, String, Int, Double, Long, Char]
// res3: List[(String, (Int, (Double, (Long, Char))))] = List((2,(1,(3.0,(4,5)))))
```
for `And`
```scala
zip5[List, And, Int, String, Double, Long, Char]
// res4: List[And[Int,And[String,And[Double,And[Long,Char]]]]] = List($anon$1$$anon$2@1e6f537)
```
And finally `Or`
```scala
zip5[List, Or, Int, String, Double, Long, Char]
// res5: List[Or[Int,Or[String,Or[Double,Or[Long,Char]]]]] = List($anon$1$$anon$2@ca044f)
```
The business logic need not change. All we need to do is make sure all of our type class instances are in scope and we can rearrange and redeclare at will. Type Driven Development is the most declarative discipline I have found; declare a bunch of types and instances, the compiler does the rest.

#Exercises
1. Define The Non-Empty Binary Tree

