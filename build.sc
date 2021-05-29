import mill._, scalalib._

object util extends ScalaModule {
  def scalaVersion = "3.0.0-RC2"
}

object domain extends ScalaModule {
  def scalaVersion = "3.0.0-RC2"
  def moduleDeps   = Seq(util)
  object test extends Tests with TestModule.ScalaTest {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.7")
  }
  object `acceptance-test` extends Tests with TestModule.ScalaTest {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.7")
  }
}
