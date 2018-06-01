# Previous Exercises
## Define the Non-Empty Binary Tree
We've nearly done it already! Take our definition of `Zip`
```scala
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
// defined trait Zip
```
We can take `Tuple2` as `G` but, what for `F`? We need a type which given an `A` ensures an `A` for any type `A`. In other words we need an Identity at the type level.
```scala
type Identity[A] = A
// defined type alias Identity
```
*Yes, this exploits an unsoundness in the type system but, we can do anything we want as long as we are careful*
```scala
implicit def nonEmptyBinaryTree: Zip[Identity, Tuple2] = new Zip[Identity, Tuple2]{
  override def zip[A, B](a: Identity[A], b: Identity[B]): Identity[(A, B)] = (a, b)
}
// nonEmptyBinaryTree: Zip[Identity,Tuple2]

implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
// zip: [F[_], G[_, _], A, B](implicit F: Zip[F,G], implicit a: F[A], implicit b: F[B])F[G[A,B]]

implicit def a: Identity[Int] = 1
// a: Identity[Int]

implicit def b: Identity[String] = "2"
// b: Identity[String]

implicit def c: Identity[Double] = 3.0
// c: Identity[Double]

implicit def d: Identity[Long] = 4L
// d: Identity[Long]

implicit def e: Identity[Char] = '5'
// e: Identity[Char]

implicitly[Identity[((Int, Long), (Char, (String, Double)))]]
// res0: Identity[((Int, Long), (Char, (String, Double)))] = ((1,4),(5,(2,3.0)))
```
__Note: When we change from `Identity` to `List` we get a `List` of non empty binary trees.__ In practice, it is a much better idea to have a singleton `List` than an `Identity` for soundness in the type system.



# The Real World
This is all well and good but eventually we have to accept user input and perform specific actions based upon that input. How can we model control structures at the type level? Well, let's start with doing it at the value level and abstract from there.
```scala
val tree1 = implicitly[List[((Char, Int), (String, Double))]]
// tree1: List[((Char, Int), (String, Double))] = List(((5,1),(2,3.0)))

val tree2 = implicitly[List[((Int, (Char, String)), (Long, Double))]]
// tree2: List[((Int, (Char, String)), (Long, Double))] = List(((1,(5,2)),(4,3.0)))

def process[F[_], G[_, _], A, B](in: F[G[A, B]]): Unit ={
  // Do whatever we want with the tree
  // Hopefully something very fancy and incredibly heroic!!!
  ()
}
// process: [F[_], G[_, _], A, B](in: F[G[A,B]])Unit

def application(id: Int): Either[String, Unit] =
  if(1 == id){
    Right(process(tree1))
  }else if(2 == id){
    Right(process(tree2))
  }else{
    Left(s"invalid tree id $id")
  }
// application: (id: Int)Either[String,Unit]
```
So, what we need is a way to bind an id to a tree and then a way to bind that id to a type. We lift everything into the type system.
```scala
trait TreeId[T]{
  def id: Int
}
// defined trait TreeId

trait TreeProcessor[Tree]{
  def process(in: Tree): Unit
}
// defined trait TreeProcessor

trait Application[Id, Tree]{
  def process(id: Int): Either[String, Unit]
}
// defined trait Application

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
}
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```

# Exercises
1. Write a function which takes an `id: Int` and returns an Either depending upon which tree is evaluated if any.
2. Write a function which constructs `TreeProcessor` instances for `Zip[List, Tuple2]` instances
