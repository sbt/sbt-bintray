package bintray

import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import sbt.Logger

object Retry {

  def withDelays[A](log: Logger, delays: Seq[Duration])(action: => A): A = {
    delays match {
      case Seq() => action
      case delay +: rest =>
        try action catch {
          case NonFatal(ex) =>
            log.warn(s"Failed with error: ${ex.getMessage}\nRetrying in $delay...")
            Thread.sleep(delay.toMillis)
            withDelays(log, rest)(action)
        }
    }
  }

}
