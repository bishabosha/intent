package intent.runner

import intent.{TestSuite, State, Stateless, AsyncState}
import intent.core.{Expectation, ExpectationResult, TestError, TestFailed, Subscriber, TestCaseResult, IntentStructure}
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}
import intent.testdata._

import scala.concurrent.{ExecutionContext, Future}

class TestSuiteRunnerTest extends TestSuite with State[TestSuiteTestCase]:
  "TestSuiteRunner" using TestSuiteTestCase() to :

    "running a stateful suite without context" using (_.noContextTestSuite) to :
      "has an error event" in:
        expectErrorMatching("^Top-level test cases".r)

    "running a suite that fails in setup" using (_.setupFailureTestSuite) to :
      "reports that 1 test was run" in :
        state =>
          whenComplete(state.runAll()) :
            case Left(_) => fail("unexpected Left")
            case Right(result) => expect(result.total).toEqual(1)

      "reports that 1 test failed" in:
        state =>
          whenComplete(state.runAll()) :
            case Left(_) => fail("unexpected Left")
            case Right(result) => expect(result.failed).toEqual(1)

      "has a failure event with the exception" in:
        state =>
          whenComplete(state.runWithEventSubscriber()) :
            case Left(_) => fail("unexpected Left")
            case Right(_) =>
              val maybeEx = state.receivedEvents().collectFirst { case TestCaseResult(_, _, TestFailed(_, Some(ex))) => ex }
              maybeEx match
                case Some(ex) => expect(ex.getMessage).toEqual("intentional failure")
                case None => fail("unexpected None")

    "running an async stateful suite that fails in setup" using (_.setupFailureAsyncTestSuite) to :
      "collects exceptions for all the failure variants" in:
        state =>
          whenComplete(state.runWithEventSubscriber()) :
            case Left(_) => fail("unexpected Left")
            case Right(_) =>
              val exceptions = state.receivedEvents().collect { case TestCaseResult(_, _, TestFailed(_, Some(ex))) => ex }
              // TODO: We need a better matcher here... Or multiple test cases!
              val combined = exceptions.map(_.getMessage).mkString("|")
              expect(combined).toEqual("intentional failure|intentional failure|intentional failure")

      "describes all the failure variants" in:
        state =>
          whenComplete(state.runWithEventSubscriber()) :
            case Left(_) => fail("unexpected Left")
            case Right(_) =>
              val messages = state.receivedEvents().collect { case TestCaseResult(_, _, TestFailed(msg, _)) => msg }
              // TODO: We need a better matcher here... Or multiple test cases!
              val combined = messages.mkString("|")
              expect(combined).toMatch("^The state setup".r) // TODO: this doesn't test all three

    "running an async stateful suite without context" using (_.noContextAsyncTestSuite) to :
      "has an error event" in:
        expectErrorMatching("^Top-level test cases".r)

    "running an empty suite" using (_.emptyTestSuite) to :
      "report that zero tests were run" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.total).toEqual(0)  // TODO: Match on case class or individual fields?

    "running the OneOfEachResultTestSuite (stateless)" using (_.oneOfEachResult) to :
      "report that totally 4 test was run" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.total).toEqual(4)

      "report that 1 test was successful" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.successful).toEqual(1)

      "report that 2 test failed" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.failed).toEqual(2)

      "report that no test had errors" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.errors).toEqual(0)

      "report that 1 test was ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(1)

      "with a registered event subscriber" using (_.copy()) to : // TODO: can we use identity here?
        "should publish 4 events" in:
          state =>
            whenComplete(state.runWithEventSubscriber()):
              _ => expect(state.receivedEvents()).toHaveLength(4)

    "running the OneOfEachResulStatefulTestSuite (stateful)" using (_.oneOfEachResultState) to:
      "report that 1 test was ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(1)

    "running the FocusedStatelessTestSuite" using (_.focusedStatelessTestSuite) to:
      "report that 1 test was run" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.successful).toEqual(1)

      "report that 1 test was ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(1)

    "running the FocusedStatefulTestSuite" using (_.focusedStatefulTestSuite) to:
      "report that 1 test was run" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.successful).toEqual(1)

      "report that 1 test was ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(1)

    "running the FocusedAsyncStatefulTestSuite" using (_.focusedAsyncTestSuite) to:
      "report that 1 test was run" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.successful).toEqual(1)

      "report that 1 test was ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(1)

    "when test suite cannot be instantiated" using (_.invalidTestSuiteClass) to :
      "a TestSuiteError should be received" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(e) => expect(s"${e.ex.getClass}: ${e.ex.getMessage}").toEqual("class java.lang.ClassNotFoundException: foo.Bar")
              case Right(_) => expect(false).toEqual(true)

    "evaluting an empty suite" using (_.emptyTestSuite) to :
      "should not be focused" in:
        state =>
          expect(state.evaluate().isFocused).toEqual(false)

    "evaluting the OneOfEachResultTestSuite" using (_.oneOfEachResult) to :
      "should not be focused" in:
        state =>
          expect(state.evaluate().isFocused).toEqual(false)

    "evaluting the FocusedStatelessTestSuite" using (_.focusedStatelessTestSuite) to :
      "should be focused" in:
        state =>
          expect(state.evaluate().isFocused).toEqual(true)

    "evaluting the FocusedStatefulTestSuite" using (_.focusedStatefulTestSuite) to :
      "should be focused" in:
        state =>
          expect(state.evaluate().isFocused).toEqual(true)

    "evaluting the FocusedStatefulTestSuite" using (_.focusedAsyncTestSuite) to :
      "should be focused" in:
        state =>
          expect(state.evaluate().isFocused).toEqual(true)

  def expectErrorMatching(re: scala.util.matching.Regex): TestSuiteTestCase => Expectation =
    state =>
      whenComplete(state.runWithEventSubscriber()) :
        case Left(_) => fail("unexpected Left")
        case Right(_) =>
          val maybeMsg = state.receivedEvents().collectFirst { case TestCaseResult(_, _, TestError(msg, _)) => msg }
          maybeMsg match
            case Some(msg) => expect(msg).toMatch("^Top-level test cases".r)
            case None => fail("unexpected None")

/**
 * Wraps a runner for a specific test suite
 */
case class TestSuiteTestCase(suiteClassName: String = null) given (ec: ExecutionContext) extends Subscriber[TestCaseResult]:

  def emptyTestSuite = TestSuiteTestCase("intent.testdata.EmtpyTestSuite")
  def invalidTestSuiteClass = TestSuiteTestCase("foo.Bar")
  def oneOfEachResult = TestSuiteTestCase("intent.runner.OneOfEachResultTestSuite")
  def oneOfEachResultState = TestSuiteTestCase("intent.runner.OneOfEachResulStatefulTestSuite")
  def setupFailureTestSuite = TestSuiteTestCase("intent.runner.StatefulFailingTestSuite")
  def setupFailureAsyncTestSuite = TestSuiteTestCase("intent.runner.StatefulFailingAsyncTestSuite")
  def noContextTestSuite = TestSuiteTestCase("intent.runner.StatefulNoContextTestSuite")
  def noContextAsyncTestSuite = TestSuiteTestCase("intent.runner.StatefulNoContextAsyncTestSuite")
  def focusedStatelessTestSuite = TestSuiteTestCase("intent.runner.FocusedStatelessTestSuite")
  def focusedStatefulTestSuite = TestSuiteTestCase("intent.runner.FocusedStatefulTestSuite")
  def focusedAsyncTestSuite = TestSuiteTestCase("intent.runner.FocusedAsyncStatefulTestSuite")

  private object lock
  val runner = new TestSuiteRunner(cl)
  var events = List[TestCaseResult]()

  def runAll(): Future[Either[TestSuiteError, TestSuiteResult]] =
    assert(suiteClassName != null, "Suite class name must be set")
    runner.runSuite(suiteClassName)

  def runWithEventSubscriber(): Future[Either[TestSuiteError, TestSuiteResult]] =
    assert(suiteClassName != null, "Suite class name must be set")
    runner.runSuite(suiteClassName, Some(this))

  def evaluate(): IntentStructure = runner.evaluateSuite(suiteClassName).right.get

  def receivedEvents(): Seq[TestCaseResult] = events

  override def onNext(event: TestCaseResult): Unit =
    lock.synchronized:
      events :+= event

  private def cl = getClass.getClassLoader

class OneOfEachResultTestSuite extends Stateless :
  "successful" in success()
  "failed" in fail("test should fail")
  "ignored" ignore success()

  "error" in:
    throw new RuntimeException("test should fail")

class OneOfEachResulStatefulTestSuite extends State[Unit] :
    "level" using (()) to:
      "ignored" ignore:
        _ => fail("Unexpected, test should be ignored")

case class StatefulFailingTestState():
    def fail: StatefulFailingTestState =
      throw new RuntimeException("intentional failure")
    def failAsync: Future[StatefulFailingTestState] =
      Future.failed(new RuntimeException("intentional failure"))
    def throwFail: Future[StatefulFailingTestState] =
      throw new RuntimeException("intentional failure")

class StatefulFailingTestSuite extends State[StatefulFailingTestState]:
  "root" using (StatefulFailingTestState()) to :
    "uh oh" using (_.fail) to :
      "won't get here" in :
        _ => expect(1).toEqual(2)

class StatefulFailingAsyncTestSuite extends AsyncState[StatefulFailingTestState]:
  "root" using (StatefulFailingTestState()) to :
    "uh oh async" usingAsync (_.failAsync) to :
      "won't get here" in :
        _ => expect(1).toEqual(2)
    "uh oh sync" using (_.fail) to :
      "won't get here" in :
        _ => expect(1).toEqual(2)
    "uh oh sync-fail-in-async" usingAsync (_.throwFail) to :
      "won't get here" in :
        _ => expect(1).toEqual(2)

class StatefulNoContextTestSuite extends State[StatefulFailingTestState]:
  "won't get here" in :
    _ => expect(1).toEqual(2)

class StatefulNoContextAsyncTestSuite extends AsyncState[StatefulFailingTestState]:
  "won't get here" in :
    _ => expect(1).toEqual(2)

class FocusedStatelessTestSuite extends Stateless:
  "should not be run" in fail("Test is not exepected to run!")
  "should be run" focus success()

class FocusedStatefulTestSuite extends State[Unit]:
  "with state" using (()) to:
    "should not be run" in:
      _ => fail("Test is not exepected to run!")
    "should be run" focus:
      _ => success()

class FocusedAsyncStatefulTestSuite extends AsyncState[Unit]:
  "with state" usingAsync (Future.successful(())) to:
    "should not be run" in:
      _ => fail("Test is not exepected to run!")
    "should be run" focus:
      _ => success()