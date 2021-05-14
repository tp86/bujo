package bujo.domain.validation

import bujo.domain.config.*
import errors.*

import bujo.util.sequenceLeft

private def validateTextLength(
    text: String,
  ): Either[NoteTextValidationError, String] =
  Either.cond(
    text.length <= MAX_NOTE_TEXT_LENGTH,
    text,
    NoteTextIsTooLongError,
  )

def validateText(text: String): Either[Seq[NoteTextValidationError], String] =
  Seq(
    validateTextLength,
  ).map(_.apply(text)).sequenceLeft
