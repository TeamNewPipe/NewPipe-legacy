package org.schabi.newpipelegacy.local.feed

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.Function4
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipelegacy.database.feed.model.FeedGroupEntity
import org.schabi.newpipelegacy.ktx.toCalendar
import org.schabi.newpipelegacy.local.feed.service.FeedEventManager
import org.schabi.newpipelegacy.local.feed.service.FeedEventManager.Event.ErrorResultEvent
import org.schabi.newpipelegacy.local.feed.service.FeedEventManager.Event.IdleEvent
import org.schabi.newpipelegacy.local.feed.service.FeedEventManager.Event.ProgressEvent
import org.schabi.newpipelegacy.local.feed.service.FeedEventManager.Event.SuccessResultEvent
import org.schabi.newpipelegacy.util.DEFAULT_THROTTLE_TIMEOUT
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class FeedViewModel(applicationContext: Context, val groupId: Long = FeedGroupEntity.GROUP_ALL_ID) : ViewModel() {
    class Factory(val context: Context, val groupId: Long = FeedGroupEntity.GROUP_ALL_ID) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FeedViewModel(context.applicationContext, groupId) as T
        }
    }

    private var feedDatabaseManager: FeedDatabaseManager = FeedDatabaseManager(applicationContext)

    private val mutableStateLiveData = MutableLiveData<FeedState>()
    val stateLiveData: LiveData<FeedState> = mutableStateLiveData

    private var combineDisposable = Flowable
        .combineLatest(
            FeedEventManager.events(),
            feedDatabaseManager.asStreamItems(groupId),
            feedDatabaseManager.notLoadedCount(groupId),
            feedDatabaseManager.oldestSubscriptionUpdate(groupId),
            Function4 { t1: FeedEventManager.Event, t2: List<StreamInfoItem>, t3: Long, t4: List<OffsetDateTime> ->
                return@Function4 CombineResultHolder(t1, t2, t3, t4.firstOrNull())
            }
        )
        .throttleLatest(DEFAULT_THROTTLE_TIMEOUT, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { (event, listFromDB, notLoadedCount, oldestUpdate) ->
            val oldestUpdateCalendar = oldestUpdate?.toCalendar()

            mutableStateLiveData.postValue(
                when (event) {
                    is IdleEvent -> FeedState.LoadedState(listFromDB, oldestUpdateCalendar, notLoadedCount)
                    is ProgressEvent -> FeedState.ProgressState(event.currentProgress, event.maxProgress, event.progressMessage)
                    is SuccessResultEvent -> FeedState.LoadedState(listFromDB, oldestUpdateCalendar, notLoadedCount, event.itemsErrors)
                    is ErrorResultEvent -> FeedState.ErrorState(event.error)
                }
            )

            if (event is ErrorResultEvent || event is SuccessResultEvent) {
                FeedEventManager.reset()
            }
        }

    override fun onCleared() {
        super.onCleared()
        combineDisposable.dispose()
    }

    private data class CombineResultHolder(val t1: FeedEventManager.Event, val t2: List<StreamInfoItem>, val t3: Long, val t4: OffsetDateTime?)
}
