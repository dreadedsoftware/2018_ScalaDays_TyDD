


# Previous Exercises
## Write a function which takes an `id: Int` and returns an Either depending upon which tree is evaluated if any.
We just need a function which dispatches these applications
```scala
trait IdOne
// defined trait IdOne

trait IdTwo
// defined trait IdTwo
```
_Note: Here we have two traits with no concrete definitions. These traits have no inhabitants, they are used to give the compiler hints during unification._
```scala
implicit def treeId1 = new TreeId[IdOne]{
  override def id: Int = 1
}
// treeId1: TreeId[IdOne]

implicit def treeId2 = new TreeId[IdTwo]{
  override def id: Int = 2
}
// treeId2: TreeId[IdTwo]

implicit def tp1 = new TreeProcessor[Tree1]{
  override def process(in: Tree1): Unit = {
    //we will handle this later
    println(in)
  }
}
// tp1: TreeProcessor[Tree1]

implicit def tp2 = new TreeProcessor[Tree2]{
  override def process(in: Tree2): Unit = {
    //we will handle this later
    println(in)
  }
}
// tp2: TreeProcessor[Tree2]

implicit def application1 = Application.process(treeId1, tree1, tp1)
// application1: Application[IdOne,Tree1]

implicit def application2 = Application.process(treeId2, tree2, tp2)
// application2: Application[IdTwo,Tree2]

def process(id: Int): Either[TreeId[IdOne], Either[TreeId[IdTwo], Unit]] =
  application1.process(id) match {
    case Right(_) => Left(treeId1)
    case Left(_) => Right{
      application2.process(id) match{
        case Right(_) => Left(treeId2)
        case Left(_) => Right(())
      }
    }
  }
// process: (id: Int)Either[TreeId[IdOne],Either[TreeId[IdTwo],Unit]]
```

## Write a function which constructs `TreeProcessor` instances for `Zip[List, Tuple2]` instances
First, a helper
```scala
trait Unzip[F[_], G[_, _]]{
  def unzip[A, B](fg: F[G[A, B]]): G[F[A], F[B]]
}
// defined trait Unzip

implicit val unzipListTuple2 = new Unzip[List, Tuple2]{
  override def unzip[A, B](fg: List[(A, B)]): (List[A], List[B]) = {
    //Do not do this in production code, this is to make the example simpler
    (fg.map(_._1), fg.map(_._2))
  }
}
// unzipListTuple2: Unzip[List,Tuple2] = $anon$1@766334
```

Now we implement our function
```scala
implicit def treeProcessor[A, B](implicit
  U: Unzip[List, Tuple2],
  tpa: TreeProcessor[List[A]],
  tpb: TreeProcessor[List[B]]): TreeProcessor[List[(A, B)]] =
  new TreeProcessor[List[(A, B)]]{
    override def process(in: List[(A, B)]): Unit = {
      val (a, b) = U.unzip(in)
      tpa.process(a)
      tpb.process(b)
    }
  }
// treeProcessor: [A, B](implicit U: Unzip[List,Tuple2], implicit tpa: TreeProcessor[List[A]], implicit tpb: TreeProcessor[List[B]])TreeProcessor[List[(A, B)]]
```

# Life-scale systems
This is cool. We can dispatch on our tree IDs and call different trees validated at compile time for different runtime input values. But what about scaling this to the real world. Surely we cannot expect to switch between 50 different trees by adding nested `Either` instances.

## Coproducts
First a type Alias
```scala
type XOR[A, B] = Either[A, B]
// defined type alias XOR
```
This way we can use `XOR` instead of `Either` so our code can have a little nicer syntax
```scala
type Things = Int XOR String XOR Double
type ThingsE = Int Either String Either Double

val things: Things = Left(Left(1))
val thingsE: ThingsE = things

val things: Things = Left(Right("1"))
val thingsE: ThingsE = things

val things: Things = Right(1.0)
val thingsE: ThingsE = things
```
So, let's do it!!! We need a type to put this all in. Let's use Application. We just need to add a new constructor to it.
```scala
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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.

import Application._
// import Application._

implicitly[Application[IdOne XOR IdTwo, Tree1 XOR Tree2]]
// res3: Application[XOR[IdOne,IdTwo],XOR[Tree1,Tree2]] = Application$$anon$2@19207a8
```
I prefer the following
```scala
type IDs = IdOne XOR IdTwo
// defined type alias IDs

type Trees = Tree1 XOR Tree2
// defined type alias Trees

implicitly[Application[IDs, Trees]]
// res4: Application[IDs,Trees] = Application$$anon$2@8e5c8f
```
This way the declaration, implementation and implicit magic are all kept separate.

XOR is a coproduct. A coproduct is a structure which differentiates between two or more possibilities. With a type like XOR we can differentiate between 3 or 4 or more by composing multiple instances together.

# Exercises
1. Rewrite the `Application` type class such that the return is `String XOR Int` where the Int is the id which matched.
2. Rewrite the `Application` constructors such that the `Left` return cases reveal which ids were tried in which order.
