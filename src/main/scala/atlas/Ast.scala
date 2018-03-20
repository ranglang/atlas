package atlas

object Ast {
  sealed abstract class Stmt extends Product with Serializable
  final case class DefnStmt(name: String, expr: Expr) extends Stmt
  final case class ExprStmt(expr: Expr) extends Stmt

  sealed abstract class Expr extends Product with Serializable
  final case class Ref(id: String) extends Expr
  final case class Block(stmts: List[Stmt], expr: Expr) extends Expr
  final case class Select(expr: Expr, field: String) extends Expr
  final case class Cond(test: Expr, trueArm: Expr, falseArm: Expr) extends Expr
  final case class Infix(op: InfixOp, arg1: Expr, arg2: Expr) extends Expr
  final case class Prefix(op: PrefixOp, arg: Expr) extends Expr
  final case class Apply(func: Expr, args: List[Expr]) extends Expr

  sealed abstract class Literal extends Expr

  object Literal {
    final case class Func(argNames: List[String], body: Expr) extends Literal {
      def arity: Int = argNames.length
    }

    sealed abstract class Data extends Literal
    final case class Obj(fields: List[(String, Expr)]) extends Data
    final case class Arr(items: List[Expr]) extends Data
    final case class Str(value: String) extends Data
    final case class Intr(value: Int) extends Data
    final case class Real(value: Double) extends Data
    final case object Null extends Data
    sealed abstract class Bool extends Data
    final case object True extends Bool
    final case object False extends Bool
  }
}
