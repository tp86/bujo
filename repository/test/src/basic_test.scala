package bujo.repository

import bujo.repository.schema.Tables._
import org.scalatest.flatspec.AnyFlatSpec
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class RepositoryTestBase extends AnyFlatSpec

class BasicRepositoryTest extends RepositoryTestBase {

  "Repository" should "be able to access tables in DB" in {
    val db = Database.forURL(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver",
    )
    try {
      val resultFuture = db.run(MTable.getTables)
      val lines        = Await.result(resultFuture, Duration.Inf)
      lines.foreach((table: MTable) => println(table))
    } finally db.close()
    assert(true)
  }
}
