```tut:invisible
type XOR[A, B] = Either[A, B]
type AND[A, B] = (A, B)

trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
implicit def zipListTuple2: Zip[List, Tuple2] = new Zip[List, Tuple2]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)

trait TreeId[T]{
  def id: Int
}
trait TreeProcessor[Tree]{
  type Out
  def process(in: Tree): Out
}
object TreeProcessor{
  type Aux[T, O] = TreeProcessor[T]{type Out = O}
}

trait Write[ToWrite]{
  def write(toWrite: ToWrite): Unit
}

trait Application[Id, Tree, Out]{
  def process(id: Int): Either[String, Int]
}
```

# Previous Exercises
## Rewrite the `Application` object to produce values of the new and improved type classes.
First, we do the basic case
```tut:book
object Application{
  implicit def process[Id, Tree, Out](implicit
    treeId: TreeId[Id],
    tree: Tree,
    tProc: TreeProcessor.Aux[Tree, Out],
    write: Write[Out]): Application[Id, Tree, Out] =
    new Application[Id, Tree, Out]{
      override def process(id: Int): Either[String, Int] = {
        if(id == treeId.id){
          write.write(tProc.process(tree))
          Right(id)
        }else{
          Left(id.toString)
        }
      }
    }
}
```
Similarly, we can follow the types to produce the functions which compose instances.
```tut:book
object Application{
  implicit def process[Id, Tree, Out](implicit
    treeId: TreeId[Id],
    tree: Tree,
    tProc: TreeProcessor.Aux[Tree, Out],
    write: Write[Out]): Application[Id, Tree, Out] =
    new Application[Id, Tree, Out]{
      override def process(id: Int): Either[String, Int] = {
        if(id == treeId.id){
          write.write(tProc.process(tree))
          Right(id)
        }else{
          Left(id.toString)
        }
      }
    }

  implicit def processCoproduct[Id1, Id2, Tree1, Tree2, Out1, Out2](implicit
    app1: Application[Id1, Tree1, Out1],
    app2: Application[Id2, Tree2, Out2]): Application[Id1 XOR Id2, Tree1 XOR Tree2, Out1 XOR Out2] =
    new Application[Id1 XOR Id2, Tree1 XOR Tree2, Out1 XOR Out2]{
      override def process(id: Int): Either[String, Int] = {
        app1.process(id) match{
          case Right(_) => Right(id)
          case Left(a) => app2.process(id) match{
            case Right(_) => Right(id)
            case Left(b) => Left(a + ", " + b)
          }
        }
      }
    }
  implicit def processProduct[Id, Tree1, Tree2, Out1, Out2](implicit
    app1: Application[Id, Tree1, Out1],
    app2: Application[Id, Tree2, Out2]): Application[Id, Tree1 AND Tree2, Out1 AND Out2] =
    new Application[Id, Tree1 AND Tree2, Out1 AND Out2]{
      override def process(id: Int): Either[String, Int] = {
        def wrong = Left(id.toString)
        def right = Right(id)
        (app1.process(id), app2.process(id)) match{
          case (Right(_), Right(_)) => right
          case (Right(_), _) => wrong
          case (_, Right(_)) => wrong
          case _ => wrong
        }
      }
    }
}
```
The fantastic thing about this style of application development is the ease with which we can augment code. We simply added a function call to the basic constructor, put our types in and the compiler did everything else for us.

## Add a `QA` type class to check the input of the `TreeProcessor` before processing and the output of the `TreeProcessor` after processing.
```tut:book
trait QA[In, Out]{
  def qaIn(in: In): Boolean
  def qaOut(out: Out): Boolean
}
```
We need two functions in our type class. Recall, type classes typically bind function values to types. There are no hard and fast rules on how many types to how many functions. We have already been working with type classes which bind a single function to multiple types, other combinations are also possible.

## Rewrite the `Application` trait and object to take advantage of `QA`.
First the trait
```tut:book
trait Application[Id, Tree, Out]{
  def process(id: Int): Either[String, Int]
}
```
Oh yeah, it is already there, we are just using already existing type parameters to do more work. Let's get into the object.

```tut:book
object Application{
  implicit def process[Id, Tree, Out](implicit
    treeId: TreeId[Id],
    tree: Tree,
    tProc: TreeProcessor.Aux[Tree, Out],
    write: Write[Out],
    qa: QA[Tree, Out]): Application[Id, Tree, Out] =
    new Application[Id, Tree, Out]{
      override def process(id: Int): Either[String, Int] = {
        if(id == treeId.id){
          val before = qa.qaIn(tree)
          if(before){
            val afterVal = tProc.process(tree)
            write.write(afterVal)
          }else Left(id.toString + ": input did not qa")
          Right(id)
        }else{
          Left(id.toString)
        }
      }
    }

  implicit def processCoproduct[Id1, Id2, Tree1, Tree2, Out1, Out2](implicit
    app1: Application[Id1, Tree1, Out1],
    app2: Application[Id2, Tree2, Out2]): Application[Id1 XOR Id2, Tree1 XOR Tree2, Out1 XOR Out2] =
    new Application[Id1 XOR Id2, Tree1 XOR Tree2, Out1 XOR Out2]{
      override def process(id: Int): Either[String, Int] = {
        app1.process(id) match{
          case Right(_) => Right(id)
          case Left(a) => app2.process(id) match{
            case Right(_) => Right(id)
            case Left(b) => Left(a + ", " + b)
          }
        }
      }
    }
  implicit def processProduct[Id, Tree1, Tree2, Out1, Out2](implicit
    app1: Application[Id, Tree1, Out1],
    app2: Application[Id, Tree2, Out2]): Application[Id, Tree1 AND Tree2, Out1 AND Out2] =
    new Application[Id, Tree1 AND Tree2, Out1 AND Out2]{
      override def process(id: Int): Either[String, Int] = {
        def wrong = Left(id.toString)
        def right = Right(id)
        (app1.process(id), app2.process(id)) match{
          case (Right(_), Right(_)) => right
          case (Right(_), _) => wrong
          case (_, Right(_)) => wrong
          case _ => wrong
        }
      }
    }
}
```
The only thing that needs to change is the basic constructor.
The composition constructors remain unchanged.
To add `QA` into our business logic, we only need to add `QA` instances
to implicit scope. This is very similar to dependency injection.

# Library Design
Scala is not a type driven language; it is object functional.
Doing type driven development in Scala takes a lot of planning
and discipline.

Thus far, we have used the mantra _Type, Define, Refine_
which is great for simple applications abstracted
out of simple functions. How do we begin to employ this discipline
from the start of a large project?

From reading a lot of code in the wild (GitHub is a treasure trove!),
I have found some similarities in all the heavily typed code people
write. Some of these things work when doing TyDD in general
(Idris, Hskell, Scala, C#, Python) and some of them are Scala specific.
We'll focus on Scala for the purposes of this workshop.

## There are only three tools
### Functions, Type Parameters, Composition
The entirety of this workshop was created with just these three ideas
in mind. IF I needed something more clever than any of these three things
I went back and did a redesign.

The usefulness of Functions is pretty self evident. Without the ability
to transform data, computers don't do very much.

Type Parameters for TyDD are used in a subtly different manner than
how they are employed in other software disciplines. In TyDD, the type
parameter is used to signal unification not simply to abstract over
data types. In our example, we used type parameters to give the compiler
the information it required to combine the parts of our application for
us.

We use composition in a few ways here. There is common function
composition where outputs match inputs and multiple operations combine
into a single larger operation. There is also composition of type classes
which is represented as larger, more complex type classes. 

## Divorce Definitions from Constructions
This can be done in any language (it is even forced in some). The idea
is data and functions can be separated. Take this example
```tut:book
class Triangle(a: Int, b: Int, c: Int){
  if(a <= 0 || b <= 0 || c <= 0) throw new Exception
  if(a+b<=c || b+c<=a || a+c<=b) throw new Exception
}
```
This constructs a triangle from three length figures. There are a couple
problems here. First, if we need to construct a triangle from any other
method we will be double validating the triangle (any new constructors
must call the original constructor). Second, there is no straight forward
way to express invalid data; we are forced to use clever notions like
exceptions and assertions (Recall we only use Functions Types and Composition).

Separating data from constructors we get
```tut:book
trait Triangle{
	def a: Int
	def b: Int
	def c: Int
}
object Triangle{
	def apply(_a: Int, _b: Int, _c: Int): Option[Triangle] = {
	  if(_a <= 0 || _b <= 0 || _c <= 0) None
	  else if(_a+_b<=_c || _b+_c<=_a || _a+_c<=_b) None
	  else Some(new Triangle{
		override def a: Int = _a
		override def b: Int = _b
		override def c: Int = _c
	  })
	}
}
```
And we can add as many constructors as we want and perform validation
without adding any extra ideas to our understanding. Yes, the initial
code is longer but, the payoffs far outweigh the startup cost.

# Function signatures are unique
This is where most of the discipline comes in. If there are two functions
with the same input types and same output type, a refinement is due. The
compiler can no longer aid us when we have ambiguous terms. Here,
type classes really shine. We can abstract the type signature into a type
class and scope the specific instances where we need them.

A nice effect of using implicits is the compiler enforces that exactly
one instance of a necessary type is in scope. It forces us to separate
concerns in our code or the code will not compile.

# Type Parameter names can be Longer than 1 Character
```tut:book
trait Either[A, B]
```
Is far less useful than
```tut:book
trait Either[First, Second]
trait Either[Primo, Secondo]
trait Either[Primero, Segundo]
trait Either[Premier, Seconde]
```
_Note: I used Google Translate for the non English left and right... hopefully it is accurate..._

This is especially important when there are 4 or more type parameters to keep
track of.

# Remember the law of least power
