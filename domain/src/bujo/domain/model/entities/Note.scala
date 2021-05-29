package bujo.domain.model

import errors.Error
import bujo.domain.api.NoteRepository

final case class Note(text: NoteText)

object Note:
  def create(text: String): Either[Seq[Error], Note] =
    NoteText.create(text) map Note.apply

  def search(textToFind: String)(using repository: NoteRepository): Seq[Note] =
    repository.getAll.filter(_.text contains textToFind)
