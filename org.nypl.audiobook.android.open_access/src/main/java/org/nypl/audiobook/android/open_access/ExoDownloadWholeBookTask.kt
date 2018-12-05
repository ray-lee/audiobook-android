package org.nypl.audiobook.android.open_access

import org.nypl.audiobook.android.api.PlayerDownloadWholeBookTaskType

/**
 * An Exo implementation of the download-whole-book task.
 */

class ExoDownloadWholeBookTask(private val audioBook: ExoAudioBook)
  : PlayerDownloadWholeBookTaskType {

  override fun fetch() {
    this.audioBook.spine.map { item -> item.downloadTask.fetch() }
  }

  override fun cancel() {
    this.audioBook.spine.map { item -> item.downloadTask.cancel() }
  }

  override fun delete() {
    this.audioBook.deleteLocalChapterData()
  }

  override val progress: Double
    get() = calculateProgress()

  private fun calculateProgress(): Double {
    return this.audioBook.spine.sumByDouble { item -> item.downloadTask.progress } / this.audioBook.spine.size
  }

}
