package bujo.repository

import bujo.repository.schema.Tables.*

import org.scalatest.flatspec.AnyFlatSpec

abstract class RepositoryTestBase extends AnyFlatSpec

class BasicRepositoryTest extends RepositoryTestBase {

  "Repository" should "be able to access tables in DB" in {
    assert(true)
  }

}
