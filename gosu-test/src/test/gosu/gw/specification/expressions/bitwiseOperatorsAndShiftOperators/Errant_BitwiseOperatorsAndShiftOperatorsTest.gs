package gw.specification.expressions.bitwiseOperatorsAndShiftOperators

class Errant_BitwiseOperatorsAndShiftOperatorsTest {
  function testBitwiseOperator() {
    var r : int

    r = ~4.0  //## issuekeys: MSG_IMPLICIT_COERCION_ERROR, MSG_BITWISE_OPERAND_MUST_BE_INT_OR_LONG
    r = ~4.0f  //## issuekeys: MSG_IMPLICIT_COERCION_ERROR, MSG_BITWISE_OPERAND_MUST_BE_INT_OR_LONG
    r = ~8
  }

  function testShiftOperators() {
    var r : int
    var i : int
    var l : long
    var b : byte
    var s : short
    var r2 : long
    var o : Object

    b = 8
    s = b >> 2  //## issuekeys: MSG_IMPLICIT_COERCION_ERROR
    r = b >> 2
    b = 126
    r = b >> 97
    r = b >> 33
    l = 126
    r = l >> 97  //## issuekeys: MSG_IMPLICIT_COERCION_ERROR
    r2 = l >> 97
    r2 = l >> 33
    b = 8
    r = b << 2
    b = -8
    r = b >> 2
    i = ~((~(-8))>>2)
    b = 8
    r = b >>> 2
    i = 8>>2
    b = -8
    r = b >>> 2
    i = (-8>>2) + (2<<~2)
    l = -8
    r2 = l >>> 2
    i = (-8>>2) + (2L<<~2)  //## issuekeys: MSG_IMPLICIT_COERCION_ERROR
    l = (-8>>2) + (2L<<~2)
    var x0 = 8 << true  //## issuekeys: MSG_BITWISE_OPERAND_MUST_BE_INT_OR_LONG
    var x1 = 8 << o  //## issuekeys: MSG_BITWISE_OPERAND_MUST_BE_INT_OR_LONG
    var x2 = 8 << "Hello"  //## issuekeys: MSG_BITWISE_OPERAND_MUST_BE_INT_OR_LONG
  }

}