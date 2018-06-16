# Testing Type Driven Libraries
Testing a Type Driven library is much different from testing a library
written with a more common object functional Scala style. Most of the
testing can be done with the compiler.

## Testing Semantics
Recall our `Zip`
```tut:silent
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
```
To test it first we need a type class instance
```tut:silent
//aliases for simplicity
type F[A] = List[A]
type G[A, B] = (A, B)
implicit def zipListTuple2: Zip[F, G] = new Zip[F, G]{
  override def zip[A, B](a: F[A], b: F[B]): F[G[A, B]] = a.zip(b)
}
```
Then we write our testpe A cases
```tut:book
implicitly[Zip[F, G]]
```
That is the test for summoning the type class instance! We create more
and more `implicitly` calls to test the rest. But first some type aliases
and `F` instances
```tut:silent
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
```tut:book
implicitly[F[A]]
implicitly[F[B]]
implicitly[F[C]]
implicitly[F[D]]

implicitly[F[G[A, A]]]
implicitly[F[G[B, B]]]
implicitly[F[G[C, C]]]
implicitly[F[G[D, D]]]

implicitly[F[G[A, B]]]
implicitly[F[G[B, A]]]

implicitly[F[G[A, G[B, G[C, D]]]]]
implicitly[F[G[G[G[A, B], C], D]]]
implicitly[F[G[G[A, B], G[C, D]]]]
```
What we are doing is testing semantics rather than functions and values.
We write tests so that when we change our library code we make sure
the semantics we designed for remain sound.

And now we test the whole thing!
```tut:silent
type XOR[A, B] = Either[A, B]
type AND[A, B] = (A, B)
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
