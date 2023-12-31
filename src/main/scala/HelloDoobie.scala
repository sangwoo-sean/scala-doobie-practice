import cats.effect.{ExitCode, IO, IOApp}
import doobie.{HC, HPS}
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update

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

  def findActorById(id: Int): IO[Option[String]] = {
    val query = sql"select name from actors where id=$id".query[String]
    query.option.transact(xa)
  }

  def findActorsStream: IO[List[String]] = {
    sql"select name from actors".query[String]
      .stream.compile.toList.transact(xa)
  }

  def findActorByName(name: String): IO[Option[Actor]] = {
    val queryString = "select id, name from actors where name = ?"
    HC.stream[Actor](
      queryString,
      HPS.set(name),
      100
    ).compile.toList.map(_.headOption).transact(xa)
  }

  def findActorsByInitial(letter: String): IO[List[Actor]] = {
    val selectPart = fr"select id, name"
    val fromPart = fr"from actors"
    val wherePart = fr"where Left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    statement.query[Actor].stream.compile.toList.transact(xa)
  }

  def saveActor(id: Int, name: String): IO[Int] = {
    val query = sql"insert into actors (id, name) VALUES ($id, $name)"
    query.update.run.transact(xa)
  }

  def saveActorAutoGenerated(name: String): IO[Int] = {
    val query = sql"insert into actors (name) VALUES ($name)"
    query.update.withUniqueGeneratedKeys[Int]("id").transact(xa)
  }

  def saveActors_v2(id: Int, name: String): IO[Int] = {
    val queryString = "insert into actors (id, name) values (?, ?)"
    Update[Actor](queryString).run(Actor(id, name)).transact(xa)
  }

  def saveActorsBulk(names: List[String]): IO[List[Actor]] = {
    val queryString = "insert into actors (name) values (?)"
    Update[String](queryString)
      .updateManyWithGeneratedKeys[Actor]("id", "name")(names).compile.toList.transact(xa)
  }

  override def run(args: List[String]): IO[ExitCode] =
    saveActorsBulk(List("lorem", "ipsum", "dolor")).map(println).as(ExitCode.Success)
}
