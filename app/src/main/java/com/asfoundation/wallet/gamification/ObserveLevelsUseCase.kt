package com.asfoundation.wallet.gamification

import com.appcoins.wallet.gamification.Gamification
import com.appcoins.wallet.gamification.repository.Levels
import com.asfoundation.wallet.wallets.usecases.GetCurrentWalletUseCase
import io.reactivex.Observable

class ObserveLevelsUseCase(private val getCurrentWallet: GetCurrentWalletUseCase,
                           private val gamification: Gamification) {

  operator fun invoke(): Observable<Levels> {
    return getCurrentWallet()
        .flatMapObservable { gamification.getLevels(it.address, true) }
  }
}