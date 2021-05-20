import pureconfig._
import sbt.Def.settingKey
import sbt.File

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
    DbConfig.default,
    SlickConfig.default,
  )
}

case class DbConfig(
    url: String,
    driver: String)
object DbConfig {
  val default: DbConfig = DbConfig("jdbc:sqlite:default.db", "org.sqlite.JDBC")
}
class SlickConfig(val profile: String)
object SlickConfig {
  def apply(profile: String): SlickConfig = new SlickConfig(profile)
  val default: SlickConfig                = SlickConfig("slick.jdbc.SQLiteProfile")
}

case class SchemasConfig(
    db: DbConfig,
    slick: SchemasSlickConfig)
    extends ProjectConfig
object SchemasConfig extends ProjectConfigDefault[SchemasConfig] {
  val default = SchemasConfig(
    DbConfig.default,
    SchemasSlickConfig(
      SlickConfig.default.profile,
      SlickCodegenConfig("generated.schema"),
    ),
  )
}

case class SlickCodegenConfig(`package`: String)
case class SchemasSlickConfig(
    override val profile: String,
    val codegen: SlickCodegenConfig)
    extends SlickConfig(profile)
