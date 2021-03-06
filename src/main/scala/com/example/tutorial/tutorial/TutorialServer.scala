package com.example.tutorial.tutorial

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.implicits._
import com.example.tutorial.tutorial.persistent.UserRepository
import com.example.tutorial.tutorial.persistent.postgresrepository.PostgresUserRepository
import doobie.free.connection.ConnectionIO
import doobie.util.transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object TutorialServer {

  def stream[F[_]: ConcurrentEffect](xa: transactor.Transactor[F])(
      implicit T: Timer[F],
      C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream

      userRepo = new PostgresUserRepository {}

      tutorial = new TutorialRoutes[F](userRepo, xa)

      httpApp = tutorial.userRoutes.orNotFound

      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8090, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
