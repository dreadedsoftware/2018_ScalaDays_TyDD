# Category Theory
## Basics
### Definition
A Category consists of.
1. objects
2. arrows
    - each arrow has an object for a source, `s`
    - each arrow has an object for a target, `t`
    - this is written `f: s -> t`
3. arrows compose
    - take an arrow `f: a -> b`
    - take another arrow `g: b -> c`
    - there is an arrow `h: a -> c` formed by joining `f` and `g`
4. composition is associative
    - given three arrows `f`, `g`, `h` that can be composed as `h(g(f))`
    - `(h(g))(f) = h(g(f))`
5. every object has an identity arrow `id: a -> a`
    - given identities `ida: a -> a`, `idb: b -> b`
    - given arrow `f: a -> b`
    - `f = f(ida) = idb(f)`

### Dual Category
The Dual, D, of a Category, C, is found by simply flipping the direction
of all arrows in C. So for each arrow `f: a -> b` in C, we have an arrow
`f': b -> a` in D.

### Functor
A functor is a morphism of Categories. Given categories C and D a Functor
from C to D would define
1. An operation which takes each object in C into an object in D
2. An operation which takes each arrow in C into an arrow in D
    - preserves the Identity
    - preserves composition

## In Our Work
Category Theory (as always) can be applied to our work to make more sense
of it. Let's take our `Zip` and `Unzip`
```tut:silent
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
trait Unzip[F[_], G[_, _]]{
  def unzip[A, B](fg: F[G[A, B]]): G[F[A], F[B]]
}
```
Taking simply the types we get
```scala
(F[A], F[B]) => F[G[A, B]]
F[G[A, B]] =>  G[F[A], F[B]]
```
Taking `Tuple2` for `G`
```scala
(F[A], F[B]) => F[(A, B)]
F[(A, B)] =>  (F[A], F[B])
```
We see, at `Tuple2` these are **Categorical Duals**. May as well make it
official.
```tut:silent
trait Zip[F[_], G[_, _]]{
  def zip[A, B](g: G[F[A], F[B]]): F[G[A, B]]
}
trait Unzip[F[_], G[_, _]]{
  def unzip[A, B](fg: F[G[A, B]]): G[F[A], F[B]]
}
```
This more aptly describes the intent of the operations. Taking a look to
some `Zip` implementations
```scala
implicit def zipListEither1: Zip[List, Either] = new Zip[List, Either]{
  override def zip[A, B](a: List[A], b: List[B]): List[Either[A, B]] = {
    if(a.size < b.size){
      a.map(Left.apply) ++
      b.drop(a.size).map(Right.apply)
    }else{
      a.take(b.size).map(Left.apply)
    }
  }
}
implicit def zipListEither2: Zip[List, Either] = new Zip[List, Either]{
  override def zip[A, B](a: List[A], b: List[B]): List[Either[A, B]] = {
    if(a.size > b.size){
      b.map(Right.apply) ++
      a.drop(b.size).map(Left.apply)
    }else{
      b.take(a.size).map(Right.apply)
    }
  }
}
```
This violates one of our design principles! We have two signatures with
wildly different results!

Our reimplementation using our new found powers of Category Theory would
be
```tut:book
implicit def zipListEither: Zip[List, Either] = new Zip[List, Either]{
  override def zip[A, B](
    e: Either[List[A], List[B]]): List[Either[A, B]] = {
    e.fold(
      _.map(Left.apply),
      _.map(Right.apply)
    )
  }
}
```
The full intent of the operation is conveyed by the type signature. This
is the only reasonable (we could return `Nil`) way to write this function.

### Traverse
What we have created performs the work of `G[F, F] => F[G]` and
`F[G] => G[F, F]`. There is an operation common to functional programming
libraries (such as scalaz and cats) referred to as a `Traverse`. This
operation is defined by `F[G] => G[F]`. What we have is a `Traverse` but
with a Bifunctor rather than a Functor. So, together `Zip` and `Unzip`
give us Bitraverse
```tut:book
trait Bitraverse[F[_], G[_, _]] extends Zip[F, G] with Unzip[F, G]
```
What this says is given a `Functor`, F, and a `Bifunctor`, G, we are
guaranteed functions which can "flip the nesting" of their composition.

### Semigroup
Take our Application combination functions
```scala
implicit def processCoproduct[Id1, Id2, Tree1, Tree2, Out1, Out2](implicit
  app1: Application[Id1, Tree1, Out1],
  app2: Application[Id2, Tree2, Out2]): Application[Id1 XOR Id2, Tree1 XOR Tree2, Out1 XOR Out2] = ???
implicit def processProduct[Id, Tree1, Tree2, Out1, Out2](implicit
  app1: Application[Id, Tree1, Out1],
  app2: Application[Id, Tree2, Out2]): Application[Id, Tree1 AND Tree2, Out1 AND Out2] = ???
```
Already a symmetry is beginning to emerge. Now, taking just the types
```scala
(Application[Id1, Tree1, Out1], Application[Id2, Tree2, Out2]) =>
  Application[Id1 XOR Id2, Tree1 XOR Tree2, Out1 XOR Out2]
(Application[Id, Tree1, Out1], Application[Id, Tree2, Out2]) =>
  Application[Id, Tree1 AND Tree2, Out1 AND Out2]
```
And the symmetry becomes a bit clearer when we remove the parameters
```scala
(Application, Application) => Application
(Application, Application) => Application
```
They look the same when viewed in this context! Much of the work in
abstraction and analysis is about looking at the body of work in a new
way or from a new perspective. This is no different.

All we are doing is taking two `Application`s and generating a new
`Application` by combining them. Again, there is an abstract idea for
this. This is a `Semigroup`. A semigroup over a structure defines a
closed, associative binary operator.

### Isomophism
When we abstract we lose data such that two seemingly dissimilar ideas
may be treated in an identical manner. We need to be cautious of this.
The above section on the semigroup looks sound but is it really? Given
three `Application`s do we have associativity? This is the same question
as is `(A, (B, C))` the same as `((A, B), C)` and is
`Either[A, Either[B, C]]` the same as `Either[Either[A, B], C]`?

In Category Theory, they are the same! The idea of isomorphism is that
given two objects, `a` and `b`, if there is an arrow, `f: a -> b`, and an
arrow, `g: b -> a`, `a` and `b` are isomorphic if and only if
`f(g) = ida` and `g(f) = idb`. If two objects are isomorphic they can
be treated as the same object. All we need to do is show our isomorphism
and we prove categorical equivalence.



## In Summation
If we continue to refine our code we typically end up with something so
abstract we cannot name it. Fortunately, a lot of really smart people
have been studying this stuff for millennia and have names ready for us!

Along with names, there is an immense body of work inside of and outside
of the software community; all we need is a willingness to be
uncomfortable and internet access to follow along.
