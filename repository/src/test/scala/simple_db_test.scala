import bujo.repository.schemas.Tables.Notes
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig._
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class DbTest extends AnyFlatSpec {

  "Test DB" should "be used during tests" in {
    val db = Database.forConfig(
      "db",
      config = ConfigSource.resources("repository.conf").config().toOption.get,
    )
    try {
      val result  = db.run(Notes.result)
      val entries = Await.result(result, 5.seconds)
      entries.foreach(println _)
    } finally db.close()
  }
}
