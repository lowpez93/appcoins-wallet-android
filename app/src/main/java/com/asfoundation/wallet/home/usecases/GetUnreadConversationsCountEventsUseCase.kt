package com.asfoundation.wallet.home.usecases

import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.UnreadConversationCountListener
import io.reactivex.Observable

class GetUnreadConversationsCountEventsUseCase {

  operator fun invoke() = Observable.create<Int> {
    it.onNext(Intercom.client().unreadConversationCount)
    val unreadListener = UnreadConversationCountListener { unreadCount -> it.onNext(unreadCount) }
    Intercom.client()
        .addUnreadConversationCountListener(unreadListener)
    it.setCancellable {
      Intercom.client()
          .removeUnreadConversationCountListener(unreadListener)
    }
  }
}