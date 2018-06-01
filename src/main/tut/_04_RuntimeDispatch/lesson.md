# Previous Exercises
## Define the Non-Empty Binary Tree
We've nearly done it already! Take our definition of `Zip`
```tut:book
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
```
We can take `Tuple2` as `G` but, what for `F`? We need a type which given an `A` ensures an `A` for any type `A`. In other words we need an Identity at the type level.
```tut:book
type Identity[A] = A
```
*Yes, this exploits an unsoundness in the type system but, we can do anything we want as long as we are careful*
```tut:book
implicit def nonEmptyBinaryTree: Zip[Identity, Tuple2] = new Zip[Identity, Tuple2]{
  override def zip[A, B](a: Identity[A], b: Identity[B]): Identity[(A, B)] = (a, b)
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
  
implicit def a: Identity[Int] = 1
implicit def b: Identity[String] = "2"
implicit def c: Identity[Double] = 3.0
implicit def d: Identity[Long] = 4L
implicit def e: Identity[Char] = '5'

implicitly[Identity[((Int, Long), (Char, (String, Double)))]]
```
__Note: When we change from `Identity` to `List` we get a `List` of non empty binary trees.__ In practice, it is a much better idea to have a singleton `List` than an `Identity` for soundness in the type system.
```tut:reset:invisible
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
implicit def zipListTuple2: Zip[List, Tuple2] = new Zip[List, Tuple2]{
  override def zip[A, B](a: List[A], b: List[B]): List[(A, B)] = a.zip(b)
}
implicit def a: List[Int] = List(1)
implicit def b: List[String] = List("2")
implicit def c: List[Double] = List(3.0)
implicit def d: List[Long] = List(4L)
implicit def e: List[Char] = List('5')
```

# The Real World
This is all well and good but eventually we have to accept user input and perform specific actions based upon that input. How can we model control structures at the type level? Well, let's start with doing it at the value level and abstract from there.
```tut:book
val tree1 = implicitly[List[((Char, Int), (String, Double))]]
val tree2 = implicitly[List[((Int, (Char, String)), (Long, Double))]]

def process[F[_], G[_, _], A, B](in: F[G[A, B]]): Unit ={
  // Do whatever we want with the tree
  // Hopefully something very fancy and incredibly heroic!!!
  ()
}
def application(id: Int): Either[String, Unit] =
  if(1 == id){
    Right(process(tree1))
  }else if(2 == id){
    Right(process(tree2))
  }else{
    Left(s"invalid tree id $id")
  }
```
So, what we need is a way to bind an id to a tree and then a way to bind that id to a type. We lift everything into the type system.
```tut:book
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
}
```

# Exercises
1. Write a function which takes an `id: Int` and returns an Either depending upon which tree is evaluated if any.
2. Write a function which constructs `TreeProcessor` instances for `Zip[List, Tuple2]` instances
