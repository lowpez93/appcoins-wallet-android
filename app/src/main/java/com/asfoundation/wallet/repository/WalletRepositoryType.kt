package com.asfoundation.wallet.repository

import com.asfoundation.wallet.entity.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal

interface WalletRepositoryType {

  fun fetchWallets(): Single<Array<Wallet>>

  fun findWallet(address: String): Single<Wallet>

  fun createWallet(password: String): Single<Wallet>

  fun restoreKeystoreToWallet(store: String, password: String,
                              newPassword: String): Single<Wallet>

  fun restorePrivateKeyToWallet(privateKey: String?, newPassword: String): Single<Wallet>

  fun exportWallet(address: String, password: String, newPassword: String?): Single<String>

  fun deleteWallet(address: String, password: String): Completable

  fun setDefaultWallet(address: String): Completable

  fun getDefaultWallet(): Single<Wallet>

  fun observeDefaultWallet(): Observable<Wallet>

  fun getEthBalanceInWei(address: String): Single<BigDecimal>

  fun getAppcBalanceInWei(address: String): Single<BigDecimal>

}