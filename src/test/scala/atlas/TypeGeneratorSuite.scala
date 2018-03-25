package atlas

import atlas.syntax._
import cats.implicits._
import minitest._
import unindent._

object TypeGeneratorSuite extends SimpleTestSuite {
  import TypeGenerator.{infixType, prefixType}

  test("constant") {
    assertSuccess(
      expr"true",
      List(
        v(0) === BoolType
      ))

    assertSuccess(
      expr"1",
      List(
        v(0) === IntType
      ))
  }

  test("cond") {
    assertSuccess(
      expr"if true then 123 else 456",
      List(
        v(0) === v(2),
        v(0) === v(3),
        v(1) === BoolType,
        v(2) === IntType,
        v(3) === IntType
      ))
  }

  test("infix") {
    assertSuccess(
      expr"123 + 456 + 789",
      List(
        FuncType(List(v(1), v(4)), v(0)) === infixType(InfixOp.Add),
        FuncType(List(v(2), v(3)), v(1)) === infixType(InfixOp.Add),
        v(2) === IntType,
        v(3) === IntType,
        v(4) === IntType
      ))
  }

  test("prefix") {
    assertSuccess(
      expr"!!true",
      List(
        FuncType(List(v(1)), v(0)) === prefixType(PrefixOp.Not),
        FuncType(List(v(2)), v(1)) === prefixType(PrefixOp.Not),
        v(2) === BoolType
      ))
  }

  test("cast") {
    assertSuccess(
      expr"1 : Int : Real",
      List(
        v(0) === DblType,
        v(0) === v(1),
        v(1) === IntType,
        v(1) === v(2),
        v(2) === IntType
      ))
  }

  test("block") {
    assertSuccess(
      expr"do 1; true end",
      List(
        v(0) === v(2),
        v(1) === IntType,
        v(2) === BoolType
      ))
  }

  test("let") {
    assertSuccess(
      expr"""
      do
        let a = 1
        let b = 2
        a > b
      end
      """,
      List(
        FuncType(List(v(1), v(2)), v(5)) === infixType(InfixOp.Gt),
        v(0) === v(5),
        v(1) === v(3),
        v(2) === v(4),
        v(3) === IntType,
        v(4) === IntType))
  }

  test("let with type") {
    assertSuccess(
      expr"do let a: Boolean = 1; a end",
      List(
        v(0) === v(1),
        v(1) === BoolType,
        v(1) === v(2),
        v(2) === IntType
      ))
  }

  test("unary func") {
    assertSuccess(
      expr"n -> n > 0",
      List(
        FuncType(List(v(1), v(3)), v(2)) === infixType(InfixOp.Gt),
        v(0) === FuncType(List(v(1)), v(2)),
        v(3) === IntType))
  }

  test("binary func") {
    assertSuccess(
      expr"(a, b) -> a > b",
      List(
        FuncType(List(v(1), v(2)), v(3)) === infixType(InfixOp.Gt),
        v(0) === FuncType(List(v(1), v(2)), v(3))))
  }

  test("typed binary func") {
    assertSuccess(
      expr"(a: Int -> String, b: Int): String -> a(b)",
      List(
        v(0) === FuncType(List(v(1), v(2)), v(3)),
        v(0) === StrType,
        v(1) === FuncType(List(IntType), StrType),
        v(1) === FuncType(List(v(2)), v(3)),
        v(2) === IntType))
  }

  test("app") {
    assertSuccess(
      prog"""
      let a = n -> n > 0
      a(10)
      """,
      List(
        FuncType(List(v(3), v(5)), v(4)) === infixType(InfixOp.Gt),
        v(0) === v(6),
        v(1) === FuncType(List(v(7)), v(6)),
        v(1) === v(2),
        v(2) === FuncType(List(v(3)), v(4)),
        v(5) === IntType,
        v(7) === IntType))
  }

  test("nested apps") {
    assertSuccess(
      expr"""
      do
        let a = n -> n + 1
        let b = n -> n > 0
        b(a(123))
      end
      """,
      List(
        FuncType(List(v(4), v(6)), v(5))  === infixType(InfixOp.Add),
        FuncType(List(v(8), v(10)), v(9)) === infixType(InfixOp.Gt),
        v(0) === v(11),
        v(1) === FuncType(List(v(13)), v(12)),
        v(1) === v(3),
        v(2) === FuncType(List(v(12)), v(11)),
        v(2) === v(7),
        v(3) === FuncType(List(v(4)), v(5)),
        v(6) === IntType,
        v(7) === FuncType(List(v(8)), v(9)),
        v(10) === IntType,
        v(13) === IntType))
  }

  test("array") {
    assertSuccess(
      expr"[]",
      List(
        v(0) === AnyType))

    assertSuccess(
      expr"[1, 2, 3]",
      List(
        v(0) === ArrType(v(1)),
        v(0) === ArrType(v(2)),
        v(0) === ArrType(v(3)),
        v(1) === IntType,
        v(2) === IntType,
        v(3) === IntType))

    assertSuccess(
      expr"a -> [1, a, 3]",
      List(
        v(0) === FuncType(List(v(1)), v(2)),
        v(2) === ArrType(v(1)),
        v(2) === ArrType(v(3)),
        v(2) === ArrType(v(4)),
        v(3) === IntType,
        v(4) === IntType))
  }

  test("block scope") {
    assertSuccess(
      expr"""
      do
        let a = 1
        do
          let a = 'hi'
          a
        end
        a
      end
      """,
      List(
        v(0) === v(1),
        v(1) === v(2),
        v(2) === IntType,
        v(3) === v(4),
        v(4) === v(5),
        v(5) === StrType))
  }

  test("func scope") {
    assertSuccess(
      expr"""
      do
        let add1 = n -> n + 1
        let apply = (a, b) -> a(b)
        apply(add1, 2)
      end
      """,
      List(
        FuncType(List(v(4), v(6)), v(5)) === infixType(InfixOp.Add),
        v(0) === v(11),
        v(1) === v(3),
        v(2) === FuncType(List(v(1), v(12)), v(11)),
        v(2) === v(7),
        v(3) === FuncType(List(v(4)), v(5)),
        v(6) === IntType,
        v(7) === FuncType(List(v(8), v(9)), v(10)),
        v(8) === FuncType(List(v(9)), v(10)),
        v(12) === IntType
        ))
  }

  test("mutual recursion") {
    assertSuccess(
      expr"""
      do
        let even = n -> if n == 0 then true  else odd(n - 1)
        let odd  = n -> if n == 0 then false else even(n - 1)
        even(10)
      end
      """,
      List(
        FuncType(List(v(13), v(16)), v(15)) === FuncType(List(IntType, IntType), BoolType),
        FuncType(List(v(13), v(20)), v(19)) === FuncType(List(IntType, IntType), IntType),
        FuncType(List(v(4), v(11)), v(10)) === FuncType(List(IntType, IntType), IntType),
        FuncType(List(v(4), v(7)), v(6)) === FuncType(List(IntType, IntType), BoolType),
        v(0) === v(21),
        v(1) === FuncType(List(v(19)), v(18)),
        v(1) === FuncType(List(v(22)), v(21)),
        v(1) === v(3),
        v(2) === FuncType(List(v(10)), v(9)),
        v(2) === v(12),
        v(3) === FuncType(List(v(4)), v(5)),
        v(5) === v(8),
        v(5) === v(9),
        v(6) === BoolType,
        v(7) === IntType,
        v(8) === BoolType,
        v(11) === IntType,
        v(12) === FuncType(List(v(13)), v(14)),
        v(14) === v(17),
        v(14) === v(18),
        v(15) === BoolType,
        v(16) === IntType,
        v(17) === BoolType,
        v(20) === IntType,
        v(22) === IntType))
  }

  def assertSuccess(expr: Expr, expected: List[Constraint], env: Env = Env.create): Unit = {
    val either = for {
      texpr  <- TypeAnnotator(expr)
      actual <- TypeGenerator(texpr)
    } yield (texpr, actual)

    either match {
      case Right((texpr, actual)) =>
        assert(
          actual == expected,
          i"""
          Incorrect results from type checking:
          expr = ${show"$expr"}
          texpr = ${show"$texpr"}
          actual =
            ${actual.mkString("\n  ")}
          expected =
            ${expected.mkString("\n  ")}
          """)

      case Left(error) =>
        fail(
          i"""
          Expected type checking to succeed, but it failed:
          $error
          """)
    }
  }

  def assertFailure(expr: Expr, expected: TypeError, env: Env = Env.create): Unit = {
    val either = for {
      texpr  <- TypeAnnotator(expr)
      actual <- TypeGenerator(texpr)
    } yield (texpr, actual)

    either match {
      case Right((texpr, actual)) =>
        fail(
          i"""
          Expected type checking to fail, but it succeeded:
          expr = ${show"$expr"}
          texpr = ${show"$texpr"}
          actual =
            ${actual.mkString("\n  ")}
          """)

      case Left(actual)                =>
        assert(
          actual == expected,
          i"""
          Type checking failed with an unexpected error:
          expr = ${expr.show}
          actual = $actual
          expected = $expected
          """)

    }
  }
}
