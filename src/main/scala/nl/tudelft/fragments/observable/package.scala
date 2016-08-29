package nl.tudelft.fragments

import rx.lang.scala.{Observable, Subscriber}

package object observable {
  implicit class RichObservable(observable: Observable[String]) {
    def split(regex: scala.util.matching.Regex): Observable[String] = observable.lift[String](o => new Subscriber[String](o) {
      private var leftOver: String = null
      private var emptyPartCount: Int = 0

      override def onCompleted = {
        if (leftOver != null) {
          output(leftOver)
        }

        if (!o.isUnsubscribed) {
          o.onCompleted()
        }
      }

      override def onNext(segment: String) = {
        val leftOverSegment = if (leftOver != null) {
          leftOver + segment
        } else {
          segment
        }

        val parts = regex.split(leftOverSegment)

        parts.init.foreach(output)

        leftOver = parts.last
      }

      private def output(part: String) = {
        if (part.isEmpty) {
          emptyPartCount = emptyPartCount + 1
        } else {
          while (emptyPartCount > 0) {
            if (!o.isUnsubscribed)  {
              o.onNext("")
            }
            emptyPartCount = emptyPartCount - 1
          }

          if (!o.isUnsubscribed) {
            o.onNext(part)
          }
        }
      }
    })
  }
}
