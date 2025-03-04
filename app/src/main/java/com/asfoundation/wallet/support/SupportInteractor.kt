package com.asfoundation.wallet.support

import com.appcoins.wallet.bdsbilling.WalletService
import com.appcoins.wallet.gamification.Gamification
import com.asfoundation.wallet.promo_code.use_cases.GetCurrentPromoCodeUseCase
import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.UnreadConversationCountListener
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.util.*

class SupportInteractor(private val supportRepository: SupportRepository,
                        private val walletService: WalletService,
                        private val gamificationRepository: Gamification,
                        private val getCurrentPromoCodeUseCase: GetCurrentPromoCodeUseCase,
                        private val viewScheduler: Scheduler,
                        private val ioScheduler: Scheduler) {

  fun showSupport(): Completable {
    return getCurrentPromoCodeUseCase()
        .flatMapCompletable { promoCode ->
          walletService.getWalletAddress()
              .flatMapCompletable { address ->
                gamificationRepository.getUserLevel(address, promoCode.code)
                    .observeOn(viewScheduler)
                    .flatMapCompletable { showSupport(address, it) }
              }
              .subscribeOn(ioScheduler)
        }
  }

  fun showSupport(gamificationLevel: Int): Completable {
    return walletService.getWalletAddress()
        .observeOn(viewScheduler)
        .flatMapCompletable { showSupport(it, gamificationLevel) }
        .subscribeOn(ioScheduler)
  }

  fun showSupport(walletAddress: String, gamificationLevel: Int): Completable {
    return Completable.fromAction {
      registerUser(gamificationLevel, walletAddress)
      displayChatScreen()
    }
  }

  fun displayChatScreen() {
    supportRepository.resetUnreadConversations()
    Intercom.client()
        .displayMessenger()
  }

  @Suppress("DEPRECATION")
  fun displayConversationListOrChat() {
    //this method was introduced because if the app is closed intercom returns 0 unread conversations
    //even if there are more
    supportRepository.resetUnreadConversations()
    val handledByIntercom = getUnreadConversations() > 0
    if (handledByIntercom) {
      Intercom.client()
          .displayMessenger()
    } else {
      Intercom.client()
          .displayConversationsList()
    }
  }

  fun registerUser(level: Int, walletAddress: String) {
    // force lowercase to make sure 2 users are not registered with the same wallet address, where
    // one has uppercase letters (to be check summed), and the other does not
    val address = walletAddress.toLowerCase(Locale.ROOT)
    val currentUser = supportRepository.getCurrentUser()
    if (currentUser.userAddress != address || currentUser.gamificationLevel != level) {
      if (currentUser.userAddress != address) {
        Intercom.client()
            .logout()
      }
      supportRepository.saveNewUser(address, level)
    }
  }

  fun hasNewUnreadConversations() =
      getUnreadConversations() > supportRepository.getSavedUnreadConversations()

  fun updateUnreadConversations() =
      supportRepository.updateUnreadConversations(Intercom.client().unreadConversationCount)

  fun getUnreadConversationCountEvents() = Observable.create<Int> {
    it.onNext(Intercom.client().unreadConversationCount)
    val unreadListener = UnreadConversationCountListener { unreadCount -> it.onNext(unreadCount) }
    Intercom.client()
        .addUnreadConversationCountListener(unreadListener)
    it.setCancellable {
      Intercom.client()
          .removeUnreadConversationCountListener(unreadListener)
    }
  }

  private fun getUnreadConversations() = Intercom.client().unreadConversationCount
}