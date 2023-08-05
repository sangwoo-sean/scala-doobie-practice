import cats.effect.{ExitCode, IO, IOApp}
import doobie.{HC, HPS}
import doobie.implicits._
import doobie.util.transactor.Transactor

case class Actor(id: Int, name: String)

case class Movie(id: String, title: String, year: Int, actors: List[String], director: String)

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

  def findActorById(id: Int) = {
    val query = sql"select name from actors where id=$id".query[String]
    query.option.transact(xa)
  }

  def findActorsStream = {
    sql"select name from actors".query[String]
      .stream.compile.toList.transact(xa)
  }

  def findActorByName(name: String) = {
    val queryString = "select id, name from actors where name = ?"
    HC.stream[Actor](
      queryString,
      HPS.set(name),
      100
    ).compile.toList.map(_.headOption).transact(xa)
  }

  def findActorsByInitial(letter: String) = {
    val selectPart = fr"select id, name"
    val fromPart = fr"from actors"
    val wherePart = fr"where Left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    statement.query[Actor].stream.compile.toList.transact(xa)
  }

  override def run(args: List[String]): IO[ExitCode] =
    findActorsByInitial("H").map(println).as(ExitCode.Success)
}
