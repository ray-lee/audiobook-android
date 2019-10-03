package org.librarysimplified.audiobook.mocking

import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadWholeBookTaskType
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.SortedMap
import java.util.concurrent.ExecutorService

/**
 * A fake audio book.
 */

class MockingAudioBook(
  override val id: PlayerBookID,
  val downloadStatusExecutor: ExecutorService,
  val downloadProvider: PlayerDownloadProviderType,
  val players: (MockingAudioBook) -> MockingPlayer) : PlayerAudioBookType {

  val statusEvents: BehaviorSubject<PlayerSpineElementDownloadStatus> = BehaviorSubject.create()
  val spineItems: MutableList<MockingSpineElement> = mutableListOf()

  private val wholeTask = MockingDownloadWholeBookTask(this)

  fun createSpineElement(id: String, title: String, duration: Duration): MockingSpineElement {
    val element = MockingSpineElement(
      bookMocking = this,
      downloadProvider = this.downloadProvider,
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadStatusEvents = this.statusEvents,
      index = spineItems.size,
      duration = duration,
      id = id,
      title = title)
    this.spineItems.add(element)
    return element
  }

  override var supportsStreaming: Boolean = false

  override val supportsIndividualChapterDeletion: Boolean
    get() = true

  override val spine: List<PlayerSpineElementType>
    get() = this.spineItems

  override val spineByID: Map<String, PlayerSpineElementType>
    get() = this.spineItems.associateBy(keySelector = { e -> e.id }, valueTransform = { e -> e })

  override val spineByPartAndChapter: SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>
    get() = sortedMapOf()

  override val spineElementDownloadStatus: Observable<PlayerSpineElementDownloadStatus>
    get() = this.statusEvents

  override val wholeBookDownloadTask: PlayerDownloadWholeBookTaskType
    get() = this.wholeTask

  override fun createPlayer(): MockingPlayer {
    return this.players.invoke(this)
  }
}