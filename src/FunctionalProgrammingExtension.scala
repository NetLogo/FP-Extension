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
    manager.addPrimitive("curry", Curry)
    manager.addPrimitive("find-indices", FindIndices)
    manager.addPrimitive("find", FindFirst)
    manager.addPrimitive("zip", ZipList)
    manager.addPrimitive("unzip", UnzipList)
    manager.addPrimitive("flatten", FlattenList)
  }
}

object TakeList extends api.Reporter {
  // Takes a number and a list
  // Returns the first n values in a given list
  override def getSyntax =
    reporterSyntax(right = List(NumberType, ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val n = try args(0).getIntValue
    catch {
      case e: api.LogoException =>
        throw new api.ExtensionException(e.getMessage)
    }
    val l = args(1).getList
    // Make sure the first argument is positive
    if (n < 0)
      throw new api.ExtensionException("First argument must be a positive number.")
    l.slice(0,n).toLogoList
  }
}


object DropList extends api.Reporter {
  // Takes a number and a list
  // Returns the last n values in a given list
  override def getSyntax =
    reporterSyntax(right = List(NumberType, ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val n = try args(0).getIntValue
    catch {
      case e: api.LogoException =>
        throw new api.ExtensionException(e.getMessage)
    }
    val l = args(1).getList
    // Make sure the first argument is positive
    if (n < 0)
      throw new api.ExtensionException("First argument must be a positive number.")
    l.slice(n,l.length).toLogoList
  }
}


object ScanList extends api.Reporter {
  // Takes a reporter and a list
  // Similar to reduce, applies the given operator to the list in order
  // but also returns the intermmittent values
  override def getSyntax =
    reporterSyntax(right = List(ReporterType,ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val lambda = args(0).getReporter
    val l = args(1).getList
    var result = Array(l(0))
    for ( i <- 0 until l.length-1) {
      result = result :+ lambda.report(context, Array(result(i),l(i+1)))
    }
    result.toLogoList
  }
}


case class ComposedReporter(reporters: List[api.AnonymousReporter]) extends api.AnonymousReporter {
  // Helper object for the compose primitive
  def syntax = reporterSyntax(right = reporters(0).syntax.right, ret = reporters.last.syntax.ret)
  def report(c: api.Context, args: Array[AnyRef]): AnyRef = {
    // Use first_reporter.syntax.right and .left for the first and last reporters to get the syntax types right
    // Call the reporters in the right order
    var cur = reporters(0).report(c, args)
    for (i <- 1 to reporters.length-1){
      cur = reporters(i).report(c, Array(cur))
    }
    cur
  }
}

object ComposeLambdas extends api.Reporter {
  // Takes a minimum of two reporters
  // Composes the given reporters together and returns the resulting reporter
  override def getSyntax =
    reporterSyntax(right = List(ReporterType, ReporterType, ReporterType | RepeatableType), defaultOption = Some(2),
      minimumOption = Some(2), ret = ReporterType)
  // Make sure that the input arguments are all reporters
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val revargs = args.reverse
    val f = try revargs(0).getReporter
    catch {
      case e: api.LogoException =>
        throw new api.ExtensionException(e.getMessage)
    }
    var reps = List(f)
    for( a <- 1 to revargs.length-1){
      val g = try revargs(a).getReporter
      catch {
        case e: api.LogoException =>
          throw new api.ExtensionException(e.getMessage)
      }
      // Throw en error message if a reporter has more than arguments
      if (g.syntax.minimum != 1 || g.syntax.isInfix){
        throw new ExtensionException("One or more reporters has an invalid number of arguments.")
      }
      reps = reps :+ g
    }
    new ComposedReporter(reps)
  }
}


object Curry extends api.Reporter {
  // Takes a reporter and input arguments
  // Converts the given reporter to a reporter with less arguments
  // by fixing the inputs to the reporter arguments
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


object FindIndices extends api.Reporter {
  // Takes a boolean reporter and a list
  // Returns all the indices in a list that reports true for the given boolean reporter
  override def getSyntax =
    reporterSyntax(right = List(ReporterType,ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val f = args(0).getReporter
    val l = args(1).getList
    val it = l.iterator
    var result = new ListBuffer[Int]()
    var i = 0
    while (it.hasNext) {
      val fi = f.report(context, Array(l(i)))
      // Make sure that the given reporter returns a boolean value
      if (!fi.isInstanceOf[Boolean]){
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


object FindFirst extends api.Reporter {
  // Takes a boolean reporter and a list
  // Returns the first value in the given list that reports true for the given reporter
  override def getSyntax =
    reporterSyntax(right = List(ReporterType,ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val f = args(0).getReporter
    val l = args(1).getList
    val it = l.iterator
    var result : Any = false
    var i = 0
    var found = false
    while (it.hasNext && !found) {
      val fi = f.report(context, Array(l(i)))
      // Make sure that the given reporter returns a boolean value
      if (!fi.isInstanceOf[Boolean]){
        throw new api.ExtensionException("The reporter does not return a boolean value.")
      }
      if (fi.asInstanceOf[Boolean]){
        found = true
      }
      it.next()
      i += 1
    }
    // Return the value that reports true if found, return false otherwise
    if (found) {
      result = l(i-1)
    } else {
      result = false
    }
    result.toLogoObject
  }
}


object ZipList extends api.Reporter {
  // Takes a minimum of one list
  // Returns a list of tuples where the nth item in each given list are paired together with each other
  override def getSyntax =
    reporterSyntax(right = List(ListType, ListType | RepeatableType), defaultOption = Some(2),
      minimumOption = Some(1), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    // Make sure it argument is a list
    val l0 = args(0).getList
    var l = List(l0)
    for( i <- 1 to args.length-1){
      val li = try args(i).getList
      catch {
        case e: api.LogoException =>
          throw new api.ExtensionException(e.getMessage)
      }
      l = l :+ li
    }
    // Find the shortest list in case they're not the same length
    var minLength = l0.length
    var minIndex = 0
    for (k <- 0 until l.length){
      if (l(k).length < minLength){
        minLength = l(k).length
        minIndex = k
      }
    }
    var result = Array[Any]()
    var subresult = Array[Any]()
    // Pairs the ith items together
    for ( i <- 0 until minLength) {
      subresult = Array[Any]()
      for (j <- 0 until l.length) {
        subresult = subresult :+ l(j)(i)
      }
      result = result :+ subresult
    }
    result.toLogoList
  }
}


object UnzipList extends api.Reporter {
  // Takes a list of lists
  // This is similar to the zip function except it takes one argument
  // and can return lists of different lengths
  override def getSyntax =
    reporterSyntax(right = List(ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val l = args(0).getList
    for( i <- 0 until l.length){
      if (!l(i).isInstanceOf[LogoList]) {
        throw new api.ExtensionException("Input must be a list of lists.")
      }
    }
    // Find the shortest list in case they're not the same length
    var maxLength = l(0).asInstanceOf[LogoList].length
    var maxIndex = 0
    for (k <- 0 until l.length){
      if (l(k).asInstanceOf[LogoList].length > maxLength){
        maxLength = l(k).asInstanceOf[LogoList].length
        maxIndex = k
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
        for (j <- 0 until l.length) {
          if (l(j).asInstanceOf[LogoList].length > i) {
            subresult = subresult :+ l(j).asInstanceOf[LogoList](i)
          }
        }
        result = result :+ subresult
        count += 1
      }
      result.toLogoList
    }
  }
}


object FlattenList extends api.Reporter {
  // Takes a list
  // Converts the list so that none of its items are in lists
  override def getSyntax =
    reporterSyntax(right = List(ListType), ret = ListType)
  def report(args: Array[api.Argument], context: api.Context): AnyRef = {
    val l = args(0).getList
    var newList = Array[Any]()
    // Recursive subfunction that ensures that there are no lists in newList
    def flatten(lst: LogoList): LogoList = {
      for ( i <- 0 until lst.length) {
        if (lst(i).isInstanceOf[LogoList]) {
          flatten(lst(i).asInstanceOf[LogoList])
        } else {
          newList = newList :+ lst(i)
        }
      }
      return LogoList(lst)
    }
    flatten(l)
    newList.toLogoList
  }
}
