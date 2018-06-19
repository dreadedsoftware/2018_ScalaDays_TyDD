import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._

object Main extends App{
  type FirstTypes = Int :: String :: Double :: HNil
  val first: FirstTypes = 1 :: "2" :: 3.0 :: HNil

  type SecondTypes = String :: String :: String :: HNil

  object mapper extends Poly1{
    implicit def caseInt = at[Int](i => "This is an int " + i)
    implicit def caseString = at[String](s => "This is a String " + s)
    implicit def caseDouble = at[Double](d => "This is a Double " + d)
  }

  mapper(12)
  mapper("12")
  mapper(12.0)

  println(first)
  println(first map mapper)

  implicitly[IsHCons.Aux[FirstTypes, Int, String :: Double :: HNil]]

  trait StringsAndInts[In <: HList]{
    type Out <: HList
  }
  object StringsAndInts{
    type Aux[In<:HList, _Out<:HList] = StringsAndInts[In]{type Out = _Out}
    implicit def base: Aux[HNil, HNil] =
      new StringsAndInts[HNil]{type Out = HNil}

    implicit def stringCase[L <: HList, Head, Tail <: HList]
      (implicit c1: IsHCons.Aux[L, Head, Tail],
        c2: IsHCons.Aux[L, String, Tail],
        si: StringsAndInts[Tail]): Aux[L, Tail] =
        new StringsAndInts[L]{type Out = Tail}

    implicit def intCase[L <: HList, Head, Tail <: HList]
      (implicit c1: IsHCons.Aux[L, Head, Tail],
        c2: IsHCons.Aux[L, Int, Tail],
        si: StringsAndInts[Tail]): Aux[L, Tail] =
        new StringsAndInts[L]{type Out = Tail}
  }

  implicitly[StringsAndInts[HNil]]
  implicitly[StringsAndInts[String :: String :: String :: HNil]]
  implicitly[StringsAndInts[Int :: Int :: Int :: HNil]]
  implicitly[StringsAndInts[Int :: String :: Int :: HNil]]
  implicitly[StringsAndInts[String :: Int :: String :: HNil]]
  // fails implicitly[StringsAndInts[String :: Char :: String :: HNil]]
}
