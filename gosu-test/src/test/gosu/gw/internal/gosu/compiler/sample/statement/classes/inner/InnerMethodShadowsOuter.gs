package gw.internal.gosu.compiler.sample.statement.classes.inner

class InnerMethodShadowsOuter
{
  function makeInner() : Inner
  {
    return new Inner()
  }

  function something() : String
  {
    return "something"
  }

  class Inner
  {
    function getSomething() : String
    {
      return something()
    }

    function getOuterSomething() : String
    {
      return outer.something()
    }

    function something() : String
    {
      return "inner something"
    }
  }
}