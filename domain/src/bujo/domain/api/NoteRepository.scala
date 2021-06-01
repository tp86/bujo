package bujo.domain.api

import errors.SavingError
import bujo.domain.model.Note
import scala.concurrent.Future

trait NoteRepository:
  def save(note: Note): Future[Either[SavingError, Note]]
  def getAll: Seq[Note]
