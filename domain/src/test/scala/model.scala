package bujo.domain.model

import errors.Error
import org.scalatest.flatspec.AnyFlatSpec

abstract class NoteTestBase extends AnyFlatSpec:
  private def failWith(errors: Seq[Error]) =
    fail(errors.map(_.message) mkString "\n")

  protected def withNote(
      eitherNote: Either[Seq[Error], Note],
    )(
      assertionBody: Note => Unit,
    ): Unit =
    eitherNote match
      case Left(errors) => failWith(errors)
      case Right(note)  => assertionBody(note)

end NoteTestBase

class NoteTest extends NoteTestBase:

  "a Note" should "be created given valid NoteText" in {
    val validText = "Some valid note text"
    val noteText  = NoteText create validText
    withNote(noteText.map(Note(_))) { note =>
      assert(note.isInstanceOf[Note])
      assert(note.text equals validText)
    }
  }

end NoteTest
