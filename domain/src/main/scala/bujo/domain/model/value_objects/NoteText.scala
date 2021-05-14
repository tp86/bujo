package bujo.domain.model.value_objects

import bujo.domain.validation.errors.ValidationError
import bujo.domain.validation.validateText

opaque type NoteText = String

object NoteText:
  private def apply(text: String): NoteText = text
  def create(text: String): Either[Seq[ValidationError], NoteText] =
    validateText(text) map NoteText.apply
