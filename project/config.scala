import pureconfig._
import sbt.File
import sbt.Def.settingKey

trait ProjectConfig
trait ProjectConfigDefault[C <: ProjectConfig] {
  implicit def self: ProjectConfigDefault[C] = this
  val default: C
}
object ProjectConfig {
  def loadConfig[C <: ProjectConfig](
      configPath: File,
    )(implicit reader: ConfigReader[C],
      configObj: ProjectConfigDefault[C],
    ): C = {
    ConfigSource
      .file(configPath)
      .load[C]
      .getOrElse(configObj.default)
  }
  lazy val projectConfig =
    settingKey[ProjectConfig]("Typesafe config file with project settings.")
}

case class RepositoryConfig(
    db: DbConfig,
    slick: SlickConfig)
    extends ProjectConfig
object RepositoryConfig extends ProjectConfigDefault[RepositoryConfig] {
  val default = RepositoryConfig(
    DbConfig("jdbc:sqlite:default.db", "org.sqlite.JDBC"),
    SlickConfig("slick.jdbc.SQLiteProfile"),
  )
}

case class DbConfig(
    url: String,
    jdbcDriver: String)
class SlickConfig(val profile: String)
object SlickConfig {
  def apply(profile: String): SlickConfig          = new SlickConfig(profile)
  def unapply(config: SlickConfig): Option[String] = Some(config.profile)
}

case class SchemasConfig(
    slick: SchemasSlickConfig)
    extends ProjectConfig

case class SlickCodegenConfig(`package`: String)
case class SchemasSlickConfig(
    override val profile: String,
    val codegen: SlickCodegenConfig)
    extends SlickConfig(profile)

object SchemasConfig extends ProjectConfigDefault[SchemasConfig] {
  val default = SchemasConfig(
    SchemasSlickConfig(
      "slick.jdbc.SQLiteProfile",
      SlickCodegenConfig("generated.schema"),
    ),
  )
}
