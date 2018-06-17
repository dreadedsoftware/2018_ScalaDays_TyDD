```tut:invisible
trait Zip[F[_], G[_, _]]{
  def zip[A, B](a: F[A], b: F[B]): F[G[A, B]]
}
implicit def zip[F[_], G[_, _], A, B](implicit F: Zip[F, G], a: F[A], b: F[B]): F[G[A, B]] =
  F.zip(a, b)
type F[A] = List[A]
type G[A, B] = (A, B)
implicit def zipListTuple2: Zip[F, G] = new Zip[F, G]{
  override def zip[A, B](a: F[A], b: F[B]): F[G[A, B]] = a.zip(b)
}
type A = Int
type B = String
type C = Double
type D = Char
implicit def fa: F[A] = List(1)
implicit def fb: F[B] = List("2")
implicit def fc: F[C] = List(3.0)
implicit def fd: F[D] = List('4')

type Tree1 = F[G[A, G[B, G[C, D]]]]
type Tree2 = F[G[G[G[A, B], C], D]]
type Tree3 = F[G[G[A, B], G[C, D]]]
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

# Previous Exercises
## Write tests for the Application.
First our instances,
```tut:silent
trait One
trait Two
trait Three

implicit def id1: TreeId[One] = TreeId[One](1)
implicit def id2: TreeId[Two] = TreeId[Two](2)
implicit def id3: TreeId[Three] = TreeId[Three](3)

implicit def proc1: TreeProcessor.Aux[Tree1, A] =
  TreeProcessor[Tree1, A](tree => 11)
implicit def proc2: TreeProcessor.Aux[Tree2, B] =
  TreeProcessor[Tree2, B](tree => "12")
implicit def proc3: TreeProcessor.Aux[Tree3, C] =
  TreeProcessor[Tree3, C](tree => 13.0)

implicit def write1: Write[A] =
  Write[A](println)
implicit def write2: Write[B] =
  Write[B](println)
implicit def write3: Write[C] =
  Write[C](println)
```
And here come the tests!
```tut:book
implicitly[Application[One, Tree1, A]]
implicitly[Application[Two, Tree2, B]]
implicitly[Application[Three, Tree3, C]]

implicitly[Application[One XOR Two, Tree1 XOR Tree2, A XOR B]]
implicitly[Application[Two, Tree2 AND Tree2, B AND B]]
```
We could test this all in one big go
```tut:silent
implicitly[Application[
  One XOR Two XOR Three,
  Tree1 XOR (Tree2 AND Tree2) XOR Tree3,
  A XOR (B AND B) XOR C]]
```
But, then we lose traceability if a small breaking change is made.

### Testing Logic
One tests type driven logic the same as any other logic. We only have two
pieces of logic `processCoproduct` and `processProduct`. Here goes
```tut:book
val processCopr =
  implicitly[Application[One XOR Two, Tree1 XOR Tree2, A XOR B]]
assert(Right(1) == processCopr.process(1))
assert(Right(2) == processCopr.process(2))
assert(Left("1, 2") == processCopr.process(3))

val processProd =
  implicitly[Application[Two, Tree2 AND Tree2, B AND B]]
assert(Left("2") == processProd.process(1))
assert(Right(2) == processProd.process(2))
assert(Left("2") == processProd.process(3))
```
And we see the results we expect are the results we find!

# Drawbacks of Type Driven Development
There are many benefits to writing code in this discipline. Our business
logic becomes super clean and the compiler enforces a certain look in
client code if we remain true to TyDD. These benefits are not free. They
come at a cost which is great in some climates and manageable in others.

## This is not Common Practice
Any organization who develops using TyDD will need to invest considerable
resources into onboarding new team members. Recent graduates from
universities and code camps will not have experience with TyDD; an internal
training program is a must. This is similar to using languages which get
little to no student exposure like Haskell, PHP and Perl.

## Barrier to Individual Productivity
This kind of code can enable a less experienced team to benefit from the
expertise of a single very experienced member. In this way, the single
member increases the productivity of the group. This on the surface seems
fantastic; afterall, the goal of teamwork is to amplify _everyone_ when
gains are made by _anyone_.

A problem emerges when changes need to be made at the library level. Often
a library is outgrown by the business that uses it, when this happens
someone needs to go in and change the library. For a team to be stable,
multiple people on the team need to know how to do any single task. If
a single person going on vacation can derail deliveries of crucial
components, your organization is in a bad place.

Before employing TyDD, make sure you have at least two team members who
are capable of maintaining the library. Also, make sure those two can
coordinate their vacation time around one another.

## Scala is Object Functional
Scala libraries are not built for this! At some point your code needs to
interact with less than typeful code (Collections, Akka, Spark). TyDD
cannot save you here. Adding TyDD does not mean you can forget about other
paradigms and practices.

In Scala, TyDD necessarily implies your codebase is now multi-paradigm.
This adds a lot to the first point here; training programs are key. Not
only do your people need to be great at TyDD; they need to be great at OOP
and great at FP. This is Scala, it will never be not Scala.

## Developers are at a Disadvantage
It has been my experience that (at least initially) your developers will
be less productive when writing code than your non-developers. For some
reason, non-developers (data scientists especially) load up the library,
read the tutorial and are on their way immediately.

The developers on the other hand trudge on week after week making little
progress until something "just clicks" and then they are ready. The effect
is far less pronounced for developers who are accustomed to untyped
languages especially python developers.

I have not had the time to really study this issue. My initial findings
lead me to believe a few things

### Developers simply have too much baggage
Especially with more experienced OOP developers; they come from a land
where their expertise buys them a great deal of comfort. Without a set of
600pg books detailing patterns and processes these guys can't seem to get
going.

### Non-developers Don't Care about Code
They add the library to their build file and don't give it another thought.
Developers often try to dissect libraries which can leave them frustrated
(recall the first time you got into the Collections API!!!). Frustration
drives productivity down.

### Dynamic Languages Teach Metaprogramming Early
Python and Ruby developers are very adept at this style of development.
Describe a very general problem in the source file and let the machine
write code which makes sense at runtime. With TyDD, you can take Scala as
an interpreted language whose interpreter is `scalac`. Now, it looks pretty
much the same as Ruby and Python.
