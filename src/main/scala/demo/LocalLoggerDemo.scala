package demo

import cats.{Applicative, FlatMap}
import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{IO, IOApp, IOLocal, Sync}
import cats.mtl.{Ask, Local}
import org.typelevel.log4cats.{Logger, LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.{Slf4jFactory, Slf4jLogger}
import cats.syntax.all._
import demo.LocalLogger.Context

object LocalLoggerDemo extends IOApp.Simple {
  def deliver[F[_]: LocalLogger: FlatMap]: F[Unit] =
    for {
      _ <- LocalLogger[F].scope(Map("trace-id" -> "id-1"))(
        post("127.0.0.1:8080")
      )
      _ <- LocalLogger[F].scope(Map("trace-id" -> "id-2"))(
        post("127.0.0.1:8081")
      )
    } yield ()

  def post[F[_]: LocalLogger](url: String): F[Unit] =
    LocalLogger[F].info(s"POST $url")

  override def run: IO[Unit] =
    LocalLogger
      .withLocal[Kleisli[IO, Context, *]]
      .flatMap { implicit localLogger => deliver }
      .run(Map.empty)

  //  override def run: IO[Unit] =
  //    LocalLogger.withIOLocal.flatMap { implicit localLogger => deliver }
}

trait LocalLogger[F[_]] {
  def info(message: String): F[Unit]
  def scope[A](ctx: Map[String, String])(fa: F[A]): F[A]
}

object LocalLogger {
  type Context = Map[String, String]

  def apply[F[_]: LocalLogger]: LocalLogger[F] = implicitly

  def withLocal[F[_]: Sync: Local[*[_], Context]]: F[LocalLogger[F]] =
    for {
      logger <- Slf4jLogger.create[F]
    } yield new LocalLogger[F] {
      override def info(message: String): F[Unit] =
        Ask.ask[F, Context].flatMap(logger.info(_)(message))

      override def scope[A](ctx: Map[String, String])(fa: F[A]): F[A] =
        Local.scope[F, Context, A](fa)(ctx)
    }

  def withIOLocal: IO[LocalLogger[IO]] =
    for {
      logger <- Slf4jLogger.create[IO]
      ioLocal <- IOLocal(Map.empty[String, String])
    } yield new LocalLogger[IO] {
      override def info(message: String): IO[Unit] =
        ioLocal.get.flatMap(logger.info(_)(message))

      override def scope[A](ctx: Map[String, String])(fa: IO[A]): IO[A] =
        Resource.make(ioLocal.getAndSet(ctx))(ioLocal.set).surround(fa)
    }
}

object Log4catsContextExample extends IOApp.Simple {
  trait ServiceA[F[_]] {
    def run: F[Unit]
  }

  object ServiceA {
    // logger prints fully qualified class name
    // passing logger initialized earlier looses contextual information
    def of[F[_]: Logger: Applicative]: F[ServiceA[F]] =
      new ServiceA[F] {
        override def run: F[Unit] = Logger[F].info("run from service A")
      }.pure[F]

    // we need Sync instantiate logger
    // logs will print fully qualified class name of the class the logger was instantiated in
    def of1[F[_]: Sync]: F[ServiceA[F]] =
      Slf4jLogger.create[F].map { logger =>
        new ServiceA[F] {
          override def run: F[Unit] = logger.info("run from service A")
        }
      }

    // we can use LoggerFactory, so that we don't have to bring powerful Sync
    def of2[F[_]: LoggerFactory: Applicative]: F[ServiceA[F]] =
      LoggerFactory.create[F].map { logger =>
        new ServiceA[F] {
          override def run: F[Unit] = logger.info("run from service A")
        }
      }
  }

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  implicit val loggerFactory: Slf4jFactory[IO] = Slf4jFactory.create[IO]

  override def run: IO[Unit] = ServiceA.of[IO].flatMap(_.run)
}
