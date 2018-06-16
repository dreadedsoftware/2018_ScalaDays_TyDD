```tut:invisible
type XOR[A, B] = Either[A, B]

trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
implicit def zipListTuple2: Zip[List, Tuple2] = new Zip[List, Tuple2]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)

implicit def a: List[Int] = List(1)
implicit def b: List[String] = List("2")
implicit def c: List[Double] = List(3.0)
implicit def d: List[Long] = List(4L)
implicit def e: List[Char] = List('5')

type Tree1 = List[((Char, Int), (String, Double))]
type Tree2 = List[((Int, (Char, String)), (Long, Double))]
val tree1 = implicitly[Tree1]
val tree2 = implicitly[Tree2]

trait TreeId[T]{
  def id: Int
}
trait TreeProcessor[Tree]{
  def process(in: Tree): Unit
}
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Unit]
}
object Application{
  def process[Id, Tree](implicit treeId: TreeId[Id], tree: Tree, tProc: TreeProcessor[Tree]): Application[Id, Tree] =
    new Application[Id, Tree]{
      override def process(id: Int): Either[String, Unit] = {
        if(id == treeId.id){
          Right(tProc.process(tree))
        }else{
          Left("unmatched")
        }
      }
    }
  implicit def process[Id1, Id2, Tree1, Tree2](implicit
    app1: Application[Id1, Tree1],
    app2: Application[Id2, Tree2]): Application[Id1 XOR Id2, Tree1 XOR Tree2] =
    new Application[Id1 XOR Id2, Tree1 XOR Tree2]{
      override def process(id: Int): Either[String, Unit] = {
        app1.process(id) match{
          case Right(_) => Right(())
          case Left(_) => app2.process(id) match{
            case Right(_) => Right(())
            case Left(_) => Left("did not match any tree")
          }
        }
      }
    }
}
```

# Previous Exercises
## Rewrite the `Application` type class such that the return is `String XOR Int` where the Int is the id which matched.
```tut:book
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Int]
}
object Application{
  def process[Id, Tree](implicit treeId: TreeId[Id], tree: Tree, tProc: TreeProcessor[Tree]): Application[Id, Tree] =
    new Application[Id, Tree]{
      override def process(id: Int): Either[String, Int] = {
        if(id == treeId.id){
          tProc.process(tree)
          Right(id)
        }else{
          Left("unmatched")
        }
      }
    }
  implicit def process[Id1, Id2, Tree1, Tree2](implicit
    app1: Application[Id1, Tree1],
    app2: Application[Id2, Tree2]): Application[Id1 XOR Id2, Tree1 XOR Tree2] =
    new Application[Id1 XOR Id2, Tree1 XOR Tree2]{
      override def process(id: Int): Either[String, Int] = {
        app1.process(id) match{
          case Right(_) => Right(id)
          case Left(_) => app2.process(id) match{
            case Right(_) => Right(id)
            case Left(_) => Left("did not match any tree")
          }
        }
      }
    }
}
```

## Rewrite the `Application` constructors such that the `Left` return cases reveal which ids were tried in which order.
```tut:book
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Int]
}
object Application{
  def process[Id, Tree](implicit treeId: TreeId[Id], tree: Tree, tProc: TreeProcessor[Tree]): Application[Id, Tree] =
    new Application[Id, Tree]{
      override def process(id: Int): Either[String, Int] = {
        if(id == treeId.id){
          tProc.process(tree)
          Right(id)
        }else{
          Left(treeId.id.toString)
        }
      }
    }
  implicit def process[Id1, Id2, Tree1, Tree2](implicit
    app1: Application[Id1, Tree1],
    app2: Application[Id2, Tree2]): Application[Id1 XOR Id2, Tree1 XOR Tree2] =
    new Application[Id1 XOR Id2, Tree1 XOR Tree2]{
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
}
```

# Products
Inevitably, we will run into a situation where we will need to process multiple trees given a single id. Where do we even begin here? Let's just pick a place and see where it takes us.

We are looking to take two `Application`s both of which have the same id and combine them into one larger `Application`.
Using our `XOR` constructor as a guide we have
```tut:book:fail
implicit def process[Id, Tree1, Tree2](implicit
  app1: Application[Id, Tree1],
  app2: Application[Id, Tree2]): Application[Id, Tree1 AND Tree2] = ???
```
We can use `Tuple2` as `AND`
```tut:book
type AND[A, B] = (A, B)

implicit def process[Id, Tree1, Tree2](implicit
  app1: Application[Id, Tree1],
  app2: Application[Id, Tree2]): Application[Id, Tree1 AND Tree2] =
  new Application[Id, Tree1 AND Tree2]{
    override def process(id: Int): Either[String, Int] = {
      def wrong(id: String) = Left(id)
      def right = Right(id)
      (app1.process(id), app2.process(id)) match{
        case (Right(_), Right(_)) => right
        case (Right(_), Left(id)) => wrong(id)
        case (Left(id), Right(_)) => wrong(id)
        case (Left(id), Left(_)) => wrong(id)
      }
    }
  }
```
So all together we have
```tut:book
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Int]
}
object Application{
  implicit def process[Id, Tree](implicit treeId: TreeId[Id], tree: Tree, tProc: TreeProcessor[Tree]): Application[Id, Tree] =
    new Application[Id, Tree]{
      override def process(id: Int): Either[String, Int] = {
        if(id == treeId.id){
          tProc.process(tree)
          Right(id)
        }else{
          Left(treeId.id.toString)
        }
      }
    }
  implicit def processCoproduct[Id1, Id2, Tree1, Tree2](implicit
    app1: Application[Id1, Tree1],
    app2: Application[Id2, Tree2]): Application[Id1 XOR Id2, Tree1 XOR Tree2] =
    new Application[Id1 XOR Id2, Tree1 XOR Tree2]{
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
  implicit def processProduct[Id, Tree1, Tree2](implicit
    app1: Application[Id, Tree1],
    app2: Application[Id, Tree2]): Application[Id, Tree1 AND Tree2] =
    new Application[Id, Tree1 AND Tree2]{
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
```
A Product is a way to declare all of some set of things simultaneously.
When we want to execute multiple trees given a single matching id,
we can use a product. Classes are an example of Products.
A class is all of its members simultaneously,
it is not simply a single one of its members.

# Exercises
1. Write three trees and make them into a single product `Application`.
2. Write two more Product `Application`s.
3. Write a Coproduct `Application` from the 3 Product `Application`s in the previous exercises.
