


# Previous Exercises
## Rewrite the `Application` object to produce values of the new and improved type classes.
First, we do the basic case
```scala
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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```
Similarly, we can follow the types to produce the functions which compose instances.
```scala
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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```
The fantastic thing about this style of application development is the ease with which we can augment code. We simply added a function call to the basic constructor, put our types in and the compiler did everything else for us.

## Add a `QA` type class to check the input of the `TreeProcessor` before processing and the output of the `TreeProcessor` after processing.
```scala
trait QA[In, Out]{
  def qaIn(in: In): Boolean
  def qaOut(out: Out): Boolean
}
// defined trait QA
```
We need two functions in our type class. Recall, type classes typically bind function values to types. There are no hard and fast rules on how many types to how many functions. We have already been working with type classes which bind a single function to multiple types, other combinations are also possible.

## Rewrite the `Application` trait and object to take advantage of `QA`.
First the trait
```scala
trait Application[Id, Tree, Out]{
  def process(id: Int): Either[String, Int]
}
// defined trait Application
// warning: previously defined object Application is not a companion to trait Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```
Oh yeah, it is already there, we are just using already existing type parameters to do more work. Let's get into the object.

```scala
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
// defined object Application
// warning: previously defined trait Application is not a companion to object Application.
// Companions must be defined together; you may wish to use :paste mode for this.
```

