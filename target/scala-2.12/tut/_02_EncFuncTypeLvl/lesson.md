# Previous exercises

Using only values and functions (no classes, no traits)
## Write a function which can zip 3 `List`s.
```scala
def zip[A, B, C](a: List[A], b: List[B], c: List[C]): List[(A, (B, C))] =
  a.zip(b.zip(c))
// zip: [A, B, C](a: List[A], b: List[B], c: List[C])List[(A, (B, C))]
```
## Write a function which can zip 3 `Vector`s.
```scala
def zip[A, B, C](a: Vector[A], b: Vector[B], c: Vector[C]): Vector[(A, (B, C))] =
  a.zip(b.zip(c))
// zip: [A, B, C](a: Vector[A], b: Vector[B], c: Vector[C])Vector[(A, (B, C))]
```

## These functions are identical! So WET ewww!
Let's abstract the container
```scala
def zip[F[_], A, B](a: F[A], b: F[B]): F[(A, B)] =
  ???
// zip: [F[_], A, B](a: F[A], b: F[B])F[(A, B)]

def zip3[F[_], A, B, C](a: F[A], b: F[B], c: F[C]): F[(A, (B, C))] =
  zip(a, zip(b, c))
// zip3: [F[_], A, B, C](a: F[A], b: F[B], c: F[C])F[(A, (B, C))]
```
We have a problem. Now we don't know what functions can be used. We no longer have a `List` or `Vector` we have instead an `F`. Let's pass a function in with the values.
```scala
def zip[F[_], A, B](a: F[A], b: F[B], f: (F[A], F[B]) => F[(A, B)]): F[(A, B)] =
  f(a, b)
// zip: [F[_], A, B](a: F[A], b: F[B], f: (F[A], F[B]) => F[(A, B)])F[(A, B)]

def zip3[F[_], A, B, C](a: F[A], b: F[B], c: F[C], f1: (F[B], F[C]) => F[(B, C)], f2: (F[A], F[(B, C)]) => F[(A, (B, C))]): F[(A, (B, C))] =
  zip(a, zip(b, c, f1), f2)
// zip3: [F[_], A, B, C](a: F[A], b: F[B], c: F[C], f1: (F[B], F[C]) => F[(B, C)], f2: (F[A], F[(B, C)]) => F[(A, (B, C))])F[(A, (B, C))]
```
Oh boy! Now the version with 3 has become super cumbersome. But they are basically the same thing, surely we can abstract this out.
```scala
trait Zip[F[_]]{
  def zip[A, B](a: F[A], b: F[B]): F[(A, B)]
}
// defined trait Zip
```
It is common practice to use a `trait` to represent a function which has been abstracted. This is referred to as a type class encoding.
So, we have
```scala
def zip[F[_], A, B](a: F[A], b: F[B], F: Zip[F]): F[(A, B)] =
  F.zip(a, b)
// zip: [F[_], A, B](a: F[A], b: F[B], F: Zip[F])F[(A, B)]

def zip3[F[_], A, B, C](a: F[A], b: F[B], c: F[C], F: Zip[F]): F[(A, (B, C))] =
  F.zip(a, F.zip(b, c))
// zip3: [F[_], A, B, C](a: F[A], b: F[B], c: F[C], F: Zip[F])F[(A, (B, C))]
```
When we abstract a value, we get a type parameter; this helps us in a sense bind a value to a type. The same thing goes for functions except we need type classes to do so. Type classes sit at a higher level of abstraction; they take type parameters.
__Note: This is not the typical definition of a type class. This definition is just super convenient for TyDD!!!__

#Exercises
1. Write a type class which is *both* A and B.
2. Write a type class which is only A or only B. 
