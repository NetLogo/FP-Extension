package org.nlogo.extensions.fp

import org.nlogo.{agent, api, core, nvm}
import core.Syntax._
import api.ScalaConversions._
import org.nlogo.api.{AnonymousReporter, Context, ExtensionException}
import org.nlogo.core.{AgentKind, LogoList, Reporter, Syntax}

import scala.collection.immutable._
import scala.collection.mutable.ListBuffer

class FunctionalProgrammingExtension extends api.DefaultClassManager {
  def load(manager: api.PrimitiveManager) {
    manager.addPrimitive("take", TakeList)
    manager.addPrimitive("drop", DropList)
    manager.addPrimitive("scan", ScanList)
    manager.addPrimitive("compose", ComposeLambdas)
    manager.addPrimitive("pipe", PipeLambdas)
    manager.addPrimitive("curry", Curry)
    manager.addPrimitive("find-indices", FindIndices)
    manager.addPrimitive("find", FindFirst)
    manager.addPrimitive("zip", ZipList)
    manager.addPrimitive("unzip", UnzipList)
    manager.addPrimitive("flatten", FlattenList)
  }
}

// Takes a number and a list
// Returns the first n values in a given list
object TakeList extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(NumberType, ListType), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val n = args(0).getIntValue
    val l = args(1).getList
    // Make sure the first argument is positive
    if (n < 0)
      throw new api.ExtensionException("First argument must be a positive number.")
    l.slice(0, n).toLogoList
  }
}


// Takes a number and a list
// Returns the last n values in a given list
object DropList extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(NumberType, ListType), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val n = args(0).getIntValue
    val l = args(1).getList
    // Make sure the first argument is positive
    if (n < 0)
      throw new api.ExtensionException("First argument must be a positive number.")
    l.slice(n, l.length).toLogoList
  }
}


// Takes a reporter and a list
// Similar to reduce, applies the given operator to the list in order
// but also returns the intermmittent values
object ScanList extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, ListType), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val lambda = args(0).getReporter
    val l = args(1).getList
    var result = Array(l(0))
    for (i <- 0 until l.length - 1) {
      result = result :+ lambda.report(context, Array(result(i), l(i + 1)))
    }
    result.toLogoList
  }
}


// Helper object for the compose primitive
case class ComposedReporter(reporters: List[api.AnonymousReporter]) extends api.AnonymousReporter {
  def syntax = reporterSyntax(right = reporters(0).syntax.right, ret = reporters.last.syntax.ret)

  def report(c: api.Context, args: Array[AnyRef]): AnyRef = {
    // Call the reporters in the right order
    var cur = reporters(0).report(c, args)
    reporters.slice(1, reporters.length).foreach { ri =>
      cur = ri.report(c, Array(cur))
    }
    cur
  }
}

// Takes a minimum of two reporters
// Composes the given reporters together and returns the resulting reporter
object ComposeLambdas extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, ReporterType, ReporterType | RepeatableType), defaultOption = Some(2),
      minimumOption = Some(2), ret = ReporterType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val revargs = args.reverse
    val f = revargs(0).getReporter
    var reps = List(f)
    // Make sure that the input arguments are all reporters
    revargs.slice(1, revargs.length).foreach { argi =>
      val g = try argi.getReporter
      catch {
        case e: api.LogoException =>
          throw new api.ExtensionException(e.getMessage)
      }
      // Throw an error message if a reporter has more than arguments
      if (g.syntax.minimum != 1 || g.syntax.isInfix) {
        throw new ExtensionException("One or more reporters have invalid number of arguments.")
      }
      reps = reps :+ g
    }
    new ComposedReporter(reps)
  }
}


// Same as compose, except it composes the reporters in reverse order
// Takes a minimum of two reporters
// Composes the given reporters together and returns the resulting reporter
object PipeLambdas extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, ReporterType, ReporterType | RepeatableType), defaultOption = Some(2),
      minimumOption = Some(2), ret = ReporterType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val f = args(0).getReporter
    var reps = List(f)
    // Make sure that the input arguments are all reporters
    args.slice(1, args.length).foreach { argi =>
      val g = try argi.getReporter
      catch {
        case e: api.LogoException =>
          throw new api.ExtensionException(e.getMessage)
      }
      // Throw an error message if a reporter has more than one arguments
      if (g.syntax.minimum != 1 || g.syntax.isInfix) {
        throw new ExtensionException("One or more reporters have invalid number of arguments.")
      }
      reps = reps :+ g
    }
    new ComposedReporter(reps)
  }
}


// Takes a reporter and input arguments
// Converts the given reporter to a reporter with less arguments
// by fixing the inputs to the reporter arguments
object Curry extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, WildcardType | RepeatableType), defaultOption = Some(2),
      minimumOption = Some(2), ret = ReporterType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val lambda = args(0).getReporter
    val v = args.drop(1).map(_.get)

    return new AnonymousReporter {
      override def report(c: Context, args: Array[AnyRef]): AnyRef = lambda.report(c, v ++ args)

      override def syntax: Syntax = reporterSyntax(right = lambda.syntax.right.drop(v.length), ret = lambda.syntax.ret)
    }
  }
}


// Takes a boolean reporter and a list
// Returns all the indices in a list that reports true for the given boolean reporter
object FindIndices extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, ListType), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val f = args(0).getReporter
    val l = args(1).getList
    val it = l.iterator
    var result = new ListBuffer[Int]()
    var i = 0
    while (it.hasNext) {
      val fi = f.report(context, Array(l(i)))
      // Make sure that the given reporter returns a boolean value
      if (!fi.isInstanceOf[Boolean]) {
        throw new api.ExtensionException("The reporter does not return a boolean value.")
      }
      if (fi.asInstanceOf[Boolean]) {
        result += i
      }
      it.next()
      i += 1
    }
    result.toLogoList
  }
}


// Takes a boolean reporter and a list
// Returns the first value in the given list that reports true for the given reporter
object FindFirst extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, ListType), ret = WildcardType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val f = args(0).getReporter
    val l = args(1).getList
    val it = l.iterator
    var result: Any = false
    var i = 0
    var found = false
    while (it.hasNext && !found) {
      val fi = f.report(context, Array(l(i)))
      // Make sure that the given reporter returns a boolean value
      if (!fi.isInstanceOf[Boolean]) {
        throw new api.ExtensionException("The reporter does not return a boolean value.")
      }
      if (fi.asInstanceOf[Boolean]) {
        found = true
      }
      it.next()
      i += 1
    }
    // Return the value that reports true if found, return false otherwise
    if (found) {
      result = l(i - 1)
    } else {
      result = false
    }
    result.toLogoObject
  }
}


// Takes a minimum of one list
// Returns a list of tuples where the nth item in each given list are paired together with each other
object ZipList extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ListType, ListType | RepeatableType), defaultOption = Some(2),
      minimumOption = Some(1), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    // Make sure it argument is a list
    val l0 = args(0).getList
    var l = List(l0)
    args.slice(1, args.length).foreach { argi =>
      val li = try argi.getList
      catch {
        case e: api.LogoException =>
          throw new api.ExtensionException(e.getMessage)
      }
      l = l :+ li
    }
    // Find the shortest list in case they're not the same length
    var minLength = l0.length
    l.foreach { lk =>
      if (lk.length < minLength) {
        minLength = lk.length
      }
    }
    var result = Array[Any]()
    var subresult = Array[Any]()
    // Pairs the ith items together
    for (i <- 0 until minLength) {
      subresult = Array[Any]()
      l.foreach { lj =>
        subresult = subresult :+ lj(i)
      }
      result = result :+ subresult
    }
    result.toLogoList
  }
}


// Takes a list of lists
// This is similar to the zip function except it takes one argument
// and can return lists of different lengths
object UnzipList extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ListType), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val l = args(0).getList
    l.foreach { li =>
      if (!li.isInstanceOf[LogoList]) {
        throw new api.ExtensionException("Input must be a list of lists.")
      }
    }
    // Find the shortest list in case they're not the same length
    var maxLength = l(0).asInstanceOf[LogoList].length
    l.foreach { lk =>
      if (lk.asInstanceOf[LogoList].length > maxLength) {
        maxLength = lk.asInstanceOf[LogoList].length
      }
    }
    // If there is only one enclosed list,  return it as it is
    if (l.length == 1) {
      l
    } else {
      // If not, pair the ith items together
      var result = Array[Any]()
      var subresult = Array[Any]()
      var count = 0
      for (i <- 0 until maxLength) {
        subresult = Array[Any]()
        l.foreach { lj =>
          if (lj.asInstanceOf[LogoList].length > i) {
            subresult = subresult :+ lj.asInstanceOf[LogoList](i)
          }
        }
        result = result :+ subresult
        count += 1
      }
      result.toLogoList
    }
  }
}


// Takes a list
// Converts the list so that none of its items are in lists
object FlattenList extends api.Reporter {
  override def getSyntax =
    reporterSyntax(right = List(ListType), ret = ListType)

  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val l = args(0).getList
    var newList = Array[Any]()

    // Recursive subfunction that ensures that there are no lists in newList
    def flatten(lst: LogoList): LogoList = {
      lst.foreach { li =>
        if (li.isInstanceOf[LogoList]) {
          flatten(li.asInstanceOf[LogoList])
        } else {
          newList = newList :+ li
        }
      }
      return LogoList(lst)
    }

    flatten(l)
    newList.toLogoList
  }
}
