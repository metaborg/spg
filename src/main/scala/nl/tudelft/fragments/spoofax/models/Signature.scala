package nl.tudelft.fragments.spoofax.models

abstract class Signature

case class OpDecl(name: String, typ: Type) extends Signature

case class OpDeclInj(typ: Type) extends Signature

abstract class Type

case class FunType(children: List[Type], result: Type) extends Type

case class ConstType(sort: Sort) extends Type
