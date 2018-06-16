# Build Options
## Higher Kinds
Scala issues a warning when code attempts to abstract over type
constructors. In Scala the syntax for such an abstraction looks something
like `F[_]` or `G[_, _]` or `Z[_[_], _[_, _]]`. Here, the letters give
a type variable and the `_` characters give a hole. We will be making
use of higher kinds a great deal, so we want to turn the warning off.

The compiler option for this is `-language:higherKinds`

## Partial Unification
Scala does not by default partially apply type constructors during
unification. This is a huge barrier to doing fancy type level coding and
can be enabled with a compiler option, `-Ypartial-unification`.
