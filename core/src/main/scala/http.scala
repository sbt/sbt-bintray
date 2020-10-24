package bintray

import com.ning.http.client.Response

case class EitherHttp[A,B](
  notFound: Response => A,
  exists: Response => B)
  extends (Response => Either[A, B]) {
  def apply(r: Response): Either[A,B] =
    if (r.getStatusCode == 404) Left(notFound(r))
    else Right(exists(r))
}
