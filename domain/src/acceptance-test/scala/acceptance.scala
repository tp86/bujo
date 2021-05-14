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
      Given("User provides valid text")
      val text: String = "Some valid note text"
      assert(validateText(text).isRight)

      When("User creates note")
      val note = Note.create(text)

      Then("Note is created")
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
      Given("User provides invalid text")
      val text = "invalid note text"
      val invalidText =
        text.repeat(text.length * (MAX_NOTE_TEXT_LENGTH / text.length) + 1)
      assert(validateText(invalidText).isLeft)

      When("User tries to create note")
      val note = Note.create(invalidText)

      Then("Note text validation error with description is returned")
      note match
        case Left(errors) =>
          assert(errors.exists(_.isInstanceOf[NoteTextValidationError]))
        case Right(_) => fail("Note should not be created with invalid text")
    }
  }
