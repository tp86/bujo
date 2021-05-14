package bujo.domain

import validation.validateText
import validation.errors.NoteTextValidationError
import model.Note
import config.MAX_NOTE_TEXT_LENGTH

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.GivenWhenThen

class DomainAcceptanceTest extends AnyFeatureSpec, GivenWhenThen:

  Feature("Note") {
    Scenario("User creates note with valid text") {
      Given("valid text")
      val text: String = "Some valid note text"
      assert(validateText(text).isRight)

      When("user creates note")
      val note = Note.create(text)

      Then("note is created")
      note match
        case Right(note) => {
          assert(note.isInstanceOf[Note])
          assert(note.text equals text)
        }
        case Left(errors) =>
          fail(
            s"Failed with errors:\n\t${errors.map(_.message) mkString "\n\t"}",
          )
    }

    Scenario("User creates note with invalid text") {
      Given("invalid text")
      val text = "invalid note text"
      val invalidText =
        text.repeat(text.length * (MAX_NOTE_TEXT_LENGTH / text.length) + 1)
      assert(validateText(invalidText).isLeft)

      When("user tries to create note")
      val note = Note.create(invalidText)

      Then("note text validation error with description is returned")
      note match
        case Left(errors) =>
          assert(errors.exists(_.isInstanceOf[NoteTextValidationError]))
        case Right(_) => fail("Note should not be created with invalid text")
    }

    Scenario("User searches for notes matching text") {
      Given("existing notes")
      val notes = List("Some note text", "Interesting text", "Interesting note")
        .map(Note.create andThen {
          case Left(errs) =>
            fail(
              s"Failed with errors:\n\t${errs.map(_.message).mkString("\n\t")}"
            )
          case Right(note) => note
        })
      import bujo.domain.api.NoteRepository
      given NoteRepository = new NoteRepository {
        def save(note: Note) = ???
        def getAll = notes
      }

      When("user searches for notes using query text")
      val notesFound = Note.search("Interesting")

      Then("notes containing query text are returned")
      assert(notesFound.size == 2)
      assert(
        notesFound
          .map(_.text)
          .containsSlice(Seq("Interesting text", "Interesting note"))
      )
    }
  }
