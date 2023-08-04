import cats.effect.{ExitCode, IO, IOApp}
import doobie.implicits._
import doobie.util.transactor.Transactor

object HelloDoobie extends IOApp {

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:hellodoobie",
    "user", // usernamec
    "1111" // password
  )

  def findAllActorsNames: IO[List[String]] = {
    val query = sql"select name from actors".query[String]
    query.to[List].transact(xa)
  }

  override def run(args: List[String]): IO[ExitCode] =
    findAllActorsNames.map(println).as(ExitCode.Success)
}
