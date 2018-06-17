# Testing Type Driven Libraries
Testing a Type Driven library is much different from testing a library
written with a more common object functional Scala style. Most of the
testing can be done with the compiler.

## Testing Semantics
Recall our `Zip`
```scala
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
```
To test it first we need a type class instance
```scala
//aliases for simplicity
type F[A] = List[A]
type G[A, B] = (A, B)
implicit def zipListTuple2: Zip[F, G] = new Zip[F, G]{
  override def zip[A, B](a: F[A], b: F[B]): F[G[A, B]] = a.zip(b)
}
```
Then we write our test cases
```scala
implicitly[Zip[F, G]]
// res1: Zip[F,G] = $anon$1@c98cd7
```
That is the test for summoning the type class instance! We create more
and more `implicitly` calls to test the rest. But first some type aliases
and `F` instances
```scala
type A = Int
type B = String
type C = Double
type D = Char
implicit def fa: F[A] = List(1)
implicit def fb: F[B] = List("2")
implicit def fc: F[C] = List(3.0)
implicit def fd: F[D] = List('4')
```
Now for the tests
```scala
implicitly[F[A]]
// res2: F[A] = List(1)

implicitly[F[B]]
// res3: F[B] = List(2)

implicitly[F[C]]
// res4: F[C] = List(3.0)

implicitly[F[D]]
// res5: F[D] = List(4)

implicitly[F[G[A, A]]]
// res6: F[G[A,A]] = List((1,1))

implicitly[F[G[B, B]]]
// res7: F[G[B,B]] = List((2,2))

implicitly[F[G[C, C]]]
// res8: F[G[C,C]] = List((3.0,3.0))

implicitly[F[G[D, D]]]
// res9: F[G[D,D]] = List((4,4))

implicitly[F[G[A, B]]]
// res10: F[G[A,B]] = List((1,2))

implicitly[F[G[B, A]]]
// res11: F[G[B,A]] = List((2,1))

type Tree1 = F[G[A, G[B, G[C, D]]]]
// defined type alias Tree1

type Tree2 = F[G[G[G[A, B], C], D]]
// defined type alias Tree2

type Tree3 = F[G[G[A, B], G[C, D]]]
// defined type alias Tree3

implicitly[Tree1]
// res12: Tree1 = List((1,(2,(3.0,4))))

implicitly[Tree2]
// res13: Tree2 = List((((1,2),3.0),4))

implicitly[Tree3]
// res14: Tree3 = List(((1,2),(3.0,4)))
```
What we are doing is testing semantics rather than functions and values.
We write tests so that when we change our library code we make sure
the semantics we designed for remain sound.

And now we test the whole thing!
```scala
type XOR[A, B] = Either[A, B]
type AND[A, B] = (A, B)
trait TreeId[T]{
  def id: Int
}
object TreeId{
  def apply[A](_id: Int): TreeId[A] = new TreeId[A]{
    override def id: Int = _id
  }
}

trait TreeProcessor[Tree]{
  type Out
  def process(in: Tree): Out
}
object TreeProcessor{
  type Aux[T, O] = TreeProcessor[T]{type Out = O}
  def apply[A, B](f: A => B): Aux[A, B] = new TreeProcessor[A]{
    type Out = B
    override def process(in: A): B = f(in)
  }
}

trait Write[ToWrite]{
  def write(toWrite: ToWrite): Unit
}
object Write{
  def apply[A](f: A => Unit): Write[A] = new Write[A]{
    override def write(a: A): Unit = f(a)
  }
}

trait Application[Id, Tree, Out]{
  def process(id: Int): Either[String, Int]
}
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
          Left(treeId.id.toString)
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
        def wrong(id: String) = Left(id)
        def right = Right(id)
        (app1.process(id), app2.process(id)) match{
          case (Right(_), Right(_)) => right
          case (Right(_), Left(id)) => wrong(id)
          case (Left(id), Right(_)) => wrong(id)
          case (Left(id), _) => wrong(id)
        }
      }
    }
}
import Application._
```
Remember, we don't have much business logic to test, just he library we
built. Because our intent is almost fully defined in the types, the
compiler can be used as an adequate testing utility. If our instances are
summoned, our code is mostly correct.

# Exercises
1. Write tests for the Application.
