package bujo.domain.validation.errors

import bujo.domain.config.*
import bujo.domain.model.errors.Error

trait ValidationError         extends Error
trait NoteTextValidationError extends ValidationError
object NoteTextIsTooLongError extends NoteTextValidationError:
  val message =
    s"Note text cannot be longer than ${MAX_NOTE_TEXT_LENGTH} characters."
