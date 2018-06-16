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
