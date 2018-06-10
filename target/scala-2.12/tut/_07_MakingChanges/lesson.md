


# Previous Exercises
## Write three trees and make them into a single product Application.
Recall our two trees
```scala
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

type Tree1 = List[((Char, Int), (String, Double))]
// defined type alias Tree1

type Tree2 = List[((Int, (Char, String)), (Long, Double))]
// defined type alias Tree2

val tree1 = implicitly[Tree1]
// tree1: Tree1 = List(((5,1),(2,3.0)))

val tree2 = implicitly[Tree2]
// tree2: Tree2 = List(((1,(5,2)),(4,3.0)))
```
Let's make a third
```scala
type Tree3 = List[(Int, (String, (Double, (Long, Char))))]
// defined type alias Tree3

val tree3 = implicitly[Tree3]
// tree3: Tree3 = List((1,(2,(3.0,(4,5)))))
```
We also need ids. First for convenience
```scala
object TreeId{
  def create[T](_id: Int): TreeId[T] = new TreeId[T]{
    override def id: Int = _id
  }
}
// defined object TreeId
// warning: previously defined trait TreeId is not a companion to object TreeId.
// Companions must be defined together; you may wish to use :paste mode for this.
```
And the IDs for our trees
```scala
trait One
// defined trait One

implicit def id1: TreeId[One] = TreeId.create[One](1)
// id1: TreeId[One]
```
Finally, the `TreeProcessor` instances for which we will add a convenience constructor
```scala
object TreeProcessor{
  def create[T](f: T => Unit): TreeProcessor[T] = new TreeProcessor[T]{
    override def process(in: T): Unit = f(in)
  }
}
// defined object TreeProcessor
// warning: previously defined trait TreeProcessor is not a companion to object TreeProcessor.
// Companions must be defined together; you may wish to use :paste mode for this.

implicit def processor1 = TreeProcessor.create[Tree1](println)
// processor1: TreeProcessor[Tree1]

implicit def processor2 = TreeProcessor.create[Tree2](println)
// processor2: TreeProcessor[Tree2]

implicit def processor3 = TreeProcessor.create[Tree3](println)
// processor3: TreeProcessor[Tree3]
```
Now, that we have all our pieces, we can build our `Application`s and combine them
```scala
import Application._
// import Application._

type App1 = Tree1 AND Tree2 AND Tree3
// defined type alias App1

implicit val app1 = implicitly[Application[One, Tree1]]
// app1: Application[One,Tree1] = Application$$anon$1@769dab

implicit val app2 = implicitly[Application[One, Tree2]]
// app2: Application[One,Tree2] = Application$$anon$1@119d333

implicit val app3 = implicitly[Application[One, Tree3]]
// app3: Application[One,Tree3] = Application$$anon$1@374a67

implicit val app = implicitly[Application[One, App1]]
// app: Application[One,App1] = Application$$anon$3@7de2f0
```

## Write two more Product trees.
Following the same pattern. First we need our `Tree`s
```scala
type Tree21 = List[(Int, (String, (Double, (Char, Long))))]
// defined type alias Tree21

type Tree22 = List[((Int, String), (Double, (Long, Char)))]
// defined type alias Tree22

type Tree23 = List[((Int, (String, Double)), (Long, Char))]
// defined type alias Tree23

implicit val tree21 = implicitly[Tree21]
// tree21: Tree21 = List((1,(2,(3.0,(5,4)))))

implicit val tree22 = implicitly[Tree22]
// tree22: Tree22 = List(((1,2),(3.0,(4,5))))

implicit val tree23 = implicitly[Tree23]
// tree23: Tree23 = List(((1,(2,3.0)),(4,5)))

type Tree31 = List[(Char, (Long, (Double, (String, Int))))]
// defined type alias Tree31

type Tree32 = List[((((Char, Long), Double), String), Int)]
// defined type alias Tree32

type Tree33 = List[(((Char, Long), Double), (String, Int))]
// defined type alias Tree33

implicit val tree31 = implicitly[Tree31]
// tree31: Tree31 = List((5,(4,(3.0,(2,1)))))

implicit val tree32 = implicitly[Tree32]
// tree32: Tree32 = List(((((5,4),3.0),2),1))

implicit val tree33 = implicitly[Tree33]
// tree33: Tree33 = List((((5,4),3.0),(2,1)))
```
Then `TreeId`s
```scala
trait Two
// defined trait Two

implicit def id2: TreeId[Two] = TreeId.create[Two](2)
// id2: TreeId[Two]

trait Three
// defined trait Three

implicit def id3: TreeId[Three] = TreeId.create[Three](3)
// id3: TreeId[Three]
```
`TreeProcessor` instances
```scala
implicit val processor21 = TreeProcessor.create[Tree21](println)
// processor21: TreeProcessor[Tree21] = TreeProcessor$$anon$1@1f5c104

implicit val processor22 = TreeProcessor.create[Tree22](println)
// processor22: TreeProcessor[Tree22] = TreeProcessor$$anon$1@edff2

implicit val processor23 = TreeProcessor.create[Tree23](println)
// processor23: TreeProcessor[Tree23] = TreeProcessor$$anon$1@17f2b4b

implicit val processor31 = TreeProcessor.create[Tree31](println)
// processor31: TreeProcessor[Tree31] = TreeProcessor$$anon$1@17bd4d3

implicit val processor32 = TreeProcessor.create[Tree32](println)
// processor32: TreeProcessor[Tree32] = TreeProcessor$$anon$1@6fe6c5

implicit val processor33 = TreeProcessor.create[Tree33](println)
// processor33: TreeProcessor[Tree33] = TreeProcessor$$anon$1@19745f5
```
Finally, we put all the pieces together
```scala
type App2 = Tree21 AND Tree22 AND Tree23
// defined type alias App2

type App3 = Tree31 AND Tree32 AND Tree33
// defined type alias App3

implicit val app21 = implicitly[Application[Two, Tree21]]
// app21: Application[Two,Tree21] = Application$$anon$1@1b58e45

implicit val app22 = implicitly[Application[Two, Tree22]]
// app22: Application[Two,Tree22] = Application$$anon$1@1f0ded5

implicit val app23 = implicitly[Application[Two, Tree23]]
// app23: Application[Two,Tree23] = Application$$anon$1@195f474

implicit val app31 = implicitly[Application[Three, Tree31]]
// app31: Application[Three,Tree31] = Application$$anon$1@127dc48

implicit val app32 = implicitly[Application[Three, Tree32]]
// app32: Application[Three,Tree32] = Application$$anon$1@32db71

implicit val app33 = implicitly[Application[Three, Tree33]]
// app33: Application[Three,Tree33] = Application$$anon$1@18c930c

implicit val app2 = implicitly[Application[Two, App2]]
// app2: Application[Two,App2] = Application$$anon$3@15cbd95

implicit val app3 = implicitly[Application[Three, App3]]
// app3: Application[Three,App3] = Application$$anon$3@a9e808
```

## Write make a Coproduct tree from the 3 Product trees in the previous exercises.
After the other two parts, this falls out pretty quickly.
```scala
val application =
  implicitly[Application[One XOR Two XOR Three, App1 XOR App2 XOR App3]]
// application: Application[XOR[XOR[One,Two],Three],XOR[XOR[App1,App2],App3]] = Application$$anon$2@6fe32d
```

# Making Changes
These Applications are super simple. All computation needs to happen in a single `TreeProcessor`. This is fine for sufficiently simple use cases but as the business grows, this library will be outgrown with it. Let's add a `Write` step to our `Application`s.

First, we need to make the `TreeProcessor` produce an output.
```scala
trait TreeProcessor[Tree]{
  type Out
  def process(in: Tree): Out
}
// defined trait TreeProcessor
// warning: previously defined object TreeProcessor is not a companion to trait TreeProcessor.
// Companions must be defined together; you may wish to use :paste mode for this.

object TreeProcessor{
  type Aux[T, O] = TreeProcessor[T]{type Out = O}
}
// defined object TreeProcessor
// warning: previously defined trait TreeProcessor is not a companion to object TreeProcessor.
// Companions must be defined together; you may wish to use :paste mode for this.
```

- Note On The Aux Pattern: On the JVM, parameterized types are not reified (type erasure). The Aux Pattern allows us to use a type member as a type parameter. This gives us the ability to reference the type far after erasure would have occurred.

Now, we need a new type class for our `Write` step.
```scala
trait Write[ToWrite]{
  def write(toWrite: ToWrite): Unit
}
// defined trait Write
```
And, our Application needs to be rewritten to take advantage of the updated `TreeProcessor` and new `Write` type classes.
```scala
trait Application[Id, Tree, Out]{
  def process(id: Int): Either[String, Int]
}
// defined trait Application
// warning: previously defined object Application is not a companion to trait Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```

# Exercises
1. Rewrite the `Application` object to produce values of the new and improved type classes.
2. Add a `QA` type class to check the input of the `TreeProcessor` before processing and the output of the `TreeProcessor` after processing.
3. Rewrite the `Application` trait and object to take advantage of `QA`.
