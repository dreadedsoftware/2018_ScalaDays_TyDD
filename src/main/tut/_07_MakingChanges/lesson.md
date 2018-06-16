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

# Previous Exercises
## Write three trees and make them into a single product Application.
Recall our two trees
```tut:book
implicit def a: List[Int] = List(1)
implicit def b: List[String] = List("2")
implicit def c: List[Double] = List(3.0)
implicit def d: List[Long] = List(4L)
implicit def e: List[Char] = List('5')

type Tree1 = List[((Char, Int), (String, Double))]
type Tree2 = List[((Int, (Char, String)), (Long, Double))]
val tree1 = implicitly[Tree1]
val tree2 = implicitly[Tree2]
```
Let's make a third
```tut:book
type Tree3 = List[(Int, (String, (Double, (Long, Char))))]
val tree3 = implicitly[Tree3]
```
We also need ids. First for convenience
```tut:book
object TreeId{
  def create[T](_id: Int): TreeId[T] = new TreeId[T]{
    override def id: Int = _id
  }
}
```
And the IDs for our trees
```tut:book
trait One
implicit def id1: TreeId[One] = TreeId.create[One](1)
```
Finally, the `TreeProcessor` instances for which we will add a convenience constructor
```tut:book
object TreeProcessor{
  def create[T](f: T => Unit): TreeProcessor[T] = new TreeProcessor[T]{
    override def process(in: T): Unit = f(in)
  }
}

implicit def processor1 = TreeProcessor.create[Tree1](println)
implicit def processor2 = TreeProcessor.create[Tree2](println)
implicit def processor3 = TreeProcessor.create[Tree3](println)
```
Now, that we have all our pieces, we can build our `Application`s and combine them
```tut:book
import Application._
type App1 = Tree1 AND Tree2 AND Tree3

implicit val app1 = implicitly[Application[One, Tree1]]
implicit val app2 = implicitly[Application[One, Tree2]]
implicit val app3 = implicitly[Application[One, Tree3]]

implicit val app = implicitly[Application[One, App1]]
```

## Write two more Product trees.
Following the same pattern. First we need our `Tree`s
```tut:book
type Tree21 = List[(Int, (String, (Double, (Char, Long))))]
type Tree22 = List[((Int, String), (Double, (Long, Char)))]
type Tree23 = List[((Int, (String, Double)), (Long, Char))]
implicit val tree21 = implicitly[Tree21]
implicit val tree22 = implicitly[Tree22]
implicit val tree23 = implicitly[Tree23]

type Tree31 = List[(Char, (Long, (Double, (String, Int))))]
type Tree32 = List[((((Char, Long), Double), String), Int)]
type Tree33 = List[(((Char, Long), Double), (String, Int))]
implicit val tree31 = implicitly[Tree31]
implicit val tree32 = implicitly[Tree32]
implicit val tree33 = implicitly[Tree33]
```
Then `TreeId`s
```tut:book
trait Two
implicit def id2: TreeId[Two] = TreeId.create[Two](2)
trait Three
implicit def id3: TreeId[Three] = TreeId.create[Three](3)
```
`TreeProcessor` instances
```tut:book
implicit val processor21 = TreeProcessor.create[Tree21](println)
implicit val processor22 = TreeProcessor.create[Tree22](println)
implicit val processor23 = TreeProcessor.create[Tree23](println)
implicit val processor31 = TreeProcessor.create[Tree31](println)
implicit val processor32 = TreeProcessor.create[Tree32](println)
implicit val processor33 = TreeProcessor.create[Tree33](println)
```
Finally, we put all the pieces together
```tut:book
type App2 = Tree21 AND Tree22 AND Tree23
type App3 = Tree31 AND Tree32 AND Tree33

implicit val app21 = implicitly[Application[Two, Tree21]]
implicit val app22 = implicitly[Application[Two, Tree22]]
implicit val app23 = implicitly[Application[Two, Tree23]]
implicit val app31 = implicitly[Application[Three, Tree31]]
implicit val app32 = implicitly[Application[Three, Tree32]]
implicit val app33 = implicitly[Application[Three, Tree33]]

implicit val app2 = implicitly[Application[Two, App2]]
implicit val app3 = implicitly[Application[Three, App3]]
```

## Write a Coproduct tree from the 3 Product trees in the previous exercises.
After the other two parts, this falls out pretty quickly.
```tut:book
val application =
  implicitly[Application[One XOR Two XOR Three, App1 XOR App2 XOR App3]]
```

# Making Changes
These Applications are super simple.
All computation needs to happen in a single `TreeProcessor`.
This is fine for sufficiently simple use cases but as the business grows,
this library will be outgrown with it.
Let's add a `Write` step to our `Application`s.

First, we need to make the `TreeProcessor` produce an output.
```tut:book
trait TreeProcessor[Tree]{
  type Out
  def process(in: Tree): Out
}
object TreeProcessor{
  type Aux[T, O] = TreeProcessor[T]{type Out = O}
}
```

- Note On The Aux Pattern: On the JVM, parameterized types are not reified (type erasure). The Aux Pattern allows us to use a type member as a type parameter. This gives us the ability to reference the type far after erasure would have occurred while holding onto the familiar type parameter syntax. Commonly, if a type is in covariant position, it is provided as a type member rather than type parameter.

Now, we need a new type class for our `Write` step.
```tut:book
trait Write[ToWrite]{
  def write(toWrite: ToWrite): Unit
}
```
And, our Application needs to be rewritten to take advantage of the updated `TreeProcessor` and new `Write` type classes.
```tut:book
trait Application[Id, Tree, Out]{
  def process(id: Int): Either[String, Int]
}
```

# Exercises
1. Rewrite the `Application` object to produce values of the new and improved type classes.
2. Add a `QA` type class to check the input of the `TreeProcessor` before processing and the output of the `TreeProcessor` after processing.
3. Rewrite the `Application` trait and object to take advantage of `QA`.
