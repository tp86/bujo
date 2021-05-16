package bujo.util

extension [A, B](eitherSeq: Seq[Either[A, B]])
  def sequenceLeft(combineRight: (B, B) => B): Either[Seq[A], B] =
    eitherSeq match
      case seq if seq.isEmpty => Left(Vector.empty)
      case Seq(either, eithers*) => {
        val eitherAcc: Either[Seq[A], B] = either match
          case Left(a)  => Left(Vector(a))
          case Right(b) => Right(b)
        eithers.foldLeft(eitherAcc) {
          (eSeq: Either[Seq[A], B], e: Either[A, B]) =>
            (eSeq, e) match
              case (Right(b1), Right(b2))     => Right(combineRight(b1, b2))
              case (left @ Left(_), Right(_)) => left
              case (Right(_), Left(a))        => Left(Vector(a))
              case (Left(as), Left(a))        => Left(as.appended(a))
        }
      }
  def sequenceLeft: Either[Seq[A], B] = eitherSeq sequenceLeft ((b, _) => b)
