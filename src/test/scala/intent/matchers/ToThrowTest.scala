package intent.matchers

import intent.{Stateless, TestSuite}
import intent.helpers.Meta

class ToThrowTest extends TestSuite with Stateless with Meta
  "toThrow":
    "with only exception type":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]()

      "should find negated match" in expect(throwIllegalState).not.toThrow[IllegalArgumentException]()

      "should detect wrong exception" in:
        runExpectation(expect(throwIllegalState).toThrow[IllegalArgumentException](),
          "Expected the code to throw java.lang.IllegalArgumentException, but it threw java.lang.IllegalStateException")

      "should detect wrong exception when negated" in:
        runExpectation(expect(throwIllegalState).not.toThrow[IllegalStateException](),
          "Expected the code not to throw java.lang.IllegalStateException, but it did")

      "should detect absence of exception" in:
        runExpectation(expect(dontThrow).toThrow[IllegalArgumentException](),
          "Expected the code to throw java.lang.IllegalArgumentException, but it did not throw anything")
    
  def throwIllegalArg = throw new IllegalArgumentException("arg error")
  def throwIllegalState = throw new IllegalStateException("state error")
  def dontThrow = "tada"

  // TODO: Test sub class also.. 