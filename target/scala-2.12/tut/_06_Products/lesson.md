


# Previous Exercises
## Rewrite the `Application` type class such that the return is `String XOR Int` where the Int is the id which matched.
```scala
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Int]
}
// defined trait Application
// warning: previously defined object Application is not a companion to trait Application.
// Companions must be defined together; you may wish to use :paste mode for this.

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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```

## Rewrite the `Application` constructors such that the `Left` return cases reveal which ids were tried in which order.
```scala
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Int]
}
// defined trait Application
// warning: previously defined object Application is not a companion to trait Application.
// Companions must be defined together; you may wish to use :paste mode for this.

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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```

# Products
Inevitably, we will run into a situation where we will need to process multiple trees given a single id. Where do we even begin here? Let's just pick a place and see where it takes us.

We are looking to take two `Application`s both of which have the same id and combine them into one larger `Application`.
Using our `XOR` constructor as a guide we have
```scala
implicit def process[Id, Tree1, Tree2](implicit
  app1: Application[Id, Tree1],
  app2: Application[Id, Tree2]): Application[Id, Tree1 AND Tree2] = ???
// <console>:24: error: not found: type AND
//          app2: Application[Id, Tree2]): Application[Id, Tree1 AND Tree2] = ???
//                                                               ^
```
We can use `Tuple2` as `AND`
```scala
type AND[A, B] = (A, B)
// defined type alias AND

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
// process: [Id, Tree1, Tree2](implicit app1: Application[Id,Tree1], implicit app2: Application[Id,Tree2])Application[Id,AND[Tree1,Tree2]]
```
So all together we have
```scala
trait Application[Id, Tree]{
  def process(id: Int): Either[String, Int]
}
// defined trait Application
// warning: previously defined object Application is not a companion to trait Application.
// Companions must be defined together; you may wish to use :paste mode for this.

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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
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
