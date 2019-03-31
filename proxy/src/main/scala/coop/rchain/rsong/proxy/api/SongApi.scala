package coop.rchain.rsong.proxy.api

import cats.effect._
import com.typesafe.scalalogging.Logger
import coop.rchain.rsong.core.domain._
import coop.rchain.rsong.proxy.moc.MocSongMetadata
import coop.rchain.rsong.proxy.repo._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import coop.rchain.rsong.proxy.moc.MocSongMetadata._
import kamon.Kamon

class SongApi[F[_]: Sync](songRepo: AssetCache, userRepo: UserCache)
    extends Http4sDsl[F] {

  object perPage extends OptionalQueryParamDecoderMatcher[Int]("per_page")

  object page extends OptionalQueryParamDecoderMatcher[Int]("page")

  object userId extends QueryParamDecoderMatcher[String]("userId")

  val log = Logger[SongApi[F]]

  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "song" :? userId(id) +& perPage(pp) +& page(p) =>
        Kamon.counter(s"200 - get /song").increment()
        Ok(mocSongs.values.toList.asJson)

      case GET -> Root / "song" / songId :? userId(uid) =>
        getSongMetadata(songId, uid) match {
          case Right(m) =>
            Kamon.counter(s"200 - get /song/$songId").increment()
            Ok(m.asJson)
          case Left(e) => computeHttpErr(e, songId, s"get /song/songId")
        }

      case GET -> Root / "song" / "music" / id =>
        Kamon.counter(s"200 - get /song/music/$id").increment()
        songRepo
          .getMemoized(id)
          .fold(
            l => {
              computeHttpErr(l, id, s"get /song/music/$id")
            },
            r => {
              Kamon.counter(s"200 - get /song/music/$id").increment()
              Ok(r.data,
                 Header("Content-Type", "binary/octet-stream"),
                 Header("Accept-Ranges", "bytes"))
            }
          )

      case GET -> Root / "art" / id ⇒
        songRepo
          .getMemoized(id)
          .fold(
            l => {
              computeHttpErr(l, id, s"get /art/$id")
            },
            r => {
              Kamon.counter(s"200 - /art/$id").increment()
              Ok(r.data,
                 Header("Content-Type", "binary/octet-stream"),
                 Header("Accept-Ranges", "bytes"))
            }
          )
    }

  private def computeHttpErr(e: Err, name: String, route: String) = {
    e.code match {
      case OpCode.nameToPar =>
        log.error(s"${e} name: ${name}, route: $route")
        Kamon.counter(s"404 - ${route}")
        NotFound(name)
      case OpCode.nameNotFound =>
        log.error(s"${e} name: ${name} , route: $route")
        Kamon.counter(s"404 - ${route}")
        NotFound(name)
      case OpCode.unregisteredUser =>
        log.error(s"${e} name: ${name} , route: $route")
        Kamon.counter(s"404 - ${route}")
        NotFound(name)
      case _ =>
        log.error(s"Server error for name: $name. ${e.toString}  route: $route")
        Kamon.counter(s"500 - ${route}")
        InternalServerError(name)
    }
  }

  val view: String => String => Either[Err, PlayCount] =
    songId =>
      userId =>
        for {
          v <- userRepo.viewPlayCount(userId)
          _ <- userRepo.decPlayCount(songId, userId)
        } yield v

  private def getSongMetadata(songId: String,
                              userId: String): Either[Err, SongResponse] = {

import cats.implicits._
    (
      MocSongMetadata.getMetadata(songId),
      view(songId)(userId)
    ).mapN(SongResponse(_, _))
  }
}
