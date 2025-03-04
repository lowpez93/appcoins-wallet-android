package com.asfoundation.wallet.ui.iab

import com.appcoins.wallet.appcoins.rewards.Transaction
import com.appcoins.wallet.bdsbilling.repository.BillingSupportedType
import com.asf.wallet.R
import com.asfoundation.wallet.analytics.FacebookEventLogger
import com.asfoundation.wallet.billing.analytics.BillingAnalytics
import com.asfoundation.wallet.entity.TransactionBuilder
import com.appcoins.wallet.commons.Logger
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.util.TransferParser
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class AppcoinsRewardsBuyPresenter(private val view: AppcoinsRewardsBuyView,
                                  private val rewardsManager: RewardsManager,
                                  private val viewScheduler: Scheduler,
                                  private val networkScheduler: Scheduler,
                                  private val disposables: CompositeDisposable,
                                  private val amount: BigDecimal,
                                  private val uri: String,
                                  private val packageName: String,
                                  private val transferParser: TransferParser,
                                  private val isBds: Boolean,
                                  private val analytics: BillingAnalytics,
                                  private val transactionBuilder: TransactionBuilder,
                                  private val formatter: CurrencyFormatUtils,
                                  private val gamificationLevel: Int,
                                  private val appcoinsRewardsBuyInteract: AppcoinsRewardsBuyInteract,
                                  private val logger: Logger) {

  companion object {
    private val TAG = AppcoinsRewardsBuyPresenter::class.java.name
  }

  fun present() {
    view.lockRotation()
    handleBuyClick()
    handleOkErrorClick()
    handleSupportClicks()
  }

  private fun handleOkErrorClick() {
    disposables.add(view.getOkErrorClick()
        .doOnNext { view.errorClose() }
        .subscribe({}, {
          logger.log(TAG, "Ok error click", it)
          view.errorClose()
        }))
  }

  private fun handleBuyClick() {
    disposables.add(
        rewardsManager.pay(
            transactionBuilder.skuId, transactionBuilder.amount(), transactionBuilder.toAddress(),
            packageName, getOrigin(isBds, transactionBuilder), transactionBuilder.type,
            transactionBuilder.payload, transactionBuilder.callbackUrl,
            transactionBuilder.orderReference, transactionBuilder.referrerUrl,
            transactionBuilder.productToken
        )
            .andThen(rewardsManager.getPaymentStatus(packageName, transactionBuilder.skuId,
                transactionBuilder.amount()))
            .observeOn(viewScheduler)
            .flatMapCompletable { paymentStatus: RewardPayment ->
              handlePaymentStatus(paymentStatus, transactionBuilder.skuId,
                  transactionBuilder.amount())
            }
            .doOnSubscribe { view.showLoading() }
            .subscribe({}, {
              logger.log(TAG, it)
              view.showError(null)
            }))
  }

  private fun getOrigin(isBds: Boolean, transaction: TransactionBuilder): String? {
    return if (transaction.origin == null) {
      if (isBds) "BDS" else null
    } else {
      transaction.origin
    }
  }

  private fun handlePaymentStatus(transaction: RewardPayment, sku: String?,
                                  amount: BigDecimal): Completable {
    sendPaymentErrorEvent(transaction)
    return when (transaction.status) {
      Status.PROCESSING -> Completable.fromAction { view.showLoading() }
      Status.COMPLETED -> {
        if (isBds && isManagedPaymentType(transactionBuilder.type)) {
          val billingType = BillingSupportedType.valueOfProductType(transactionBuilder.type)
          rewardsManager.getPaymentCompleted(packageName, sku, transaction.purchaseUid, billingType)
              .flatMapCompletable { purchase ->
                Completable.fromAction { view.showTransactionCompleted() }
                    .subscribeOn(viewScheduler)
                    .andThen(Completable.timer(view.getAnimationDuration(), TimeUnit.MILLISECONDS))
                    .andThen(
                        Completable.fromAction { appcoinsRewardsBuyInteract.removeAsyncLocalPayment() })
                    .andThen(Completable.fromAction {
                      view.finish(purchase, transaction.orderReference)
                    })
              }
              .observeOn(viewScheduler)
              .onErrorResumeNext {
                Completable.fromAction {
                  logger.log(TAG, "Error after completing the transaction", it)
                  view.showError(null)
                  view.hideLoading()
                }
              }
        } else {
          rewardsManager.getTransaction(packageName, sku, amount)
              .firstOrError()
              .map(Transaction::txId)
              .flatMapCompletable { transactionId ->
                Completable.fromAction { view.showTransactionCompleted() }
                    .subscribeOn(viewScheduler)
                    .andThen(Completable.timer(view.getAnimationDuration(), TimeUnit.MILLISECONDS))
                    .andThen(Completable.fromAction { view.finish(transactionId) })
              }
        }
      }
      Status.ERROR -> Completable.fromAction {
        logger.log(TAG, "Credits error: ${transaction.errorMessage}")
        view.showError(null)
      }
      Status.FORBIDDEN -> Completable.fromAction {
        logger.log(TAG, "Forbidden")
        handleFraudFlow()
      }
      Status.SUB_ALREADY_OWNED -> Completable.fromAction {
        logger.log(TAG, "Sub already owned")
        view.showError(R.string.subscriptions_error_already_subscribed)
      }
      Status.NO_NETWORK -> Completable.fromAction {
        view.showNoNetworkError()
        view.hideLoading()
      }
    }
  }

  private fun handleFraudFlow() {
    disposables.add(
        appcoinsRewardsBuyInteract.isWalletBlocked()
            .subscribeOn(networkScheduler)
            .observeOn(networkScheduler)
            .flatMap { blocked ->
              if (blocked) {
                appcoinsRewardsBuyInteract.isWalletVerified()
                    .observeOn(viewScheduler)
                    .doOnSuccess {
                      if (it) view.showError(R.string.purchase_error_wallet_block_code_403)
                      else view.showVerification()
                    }
              } else {
                Single.just(true)
                    .observeOn(viewScheduler)
                    .doOnSuccess { view.showError(R.string.purchase_error_wallet_block_code_403) }
              }
            }
            .observeOn(viewScheduler)
            .subscribe({}, {
              logger.log(TAG, it)
              view.showError(R.string.purchase_error_wallet_block_code_403)
            })
    )
  }

  fun stop() = disposables.clear()

  fun sendPaymentEvent() {
    analytics.sendPaymentEvent(packageName, transactionBuilder.skuId,
        transactionBuilder.amount()
            .toString(), BillingAnalytics.PAYMENT_METHOD_REWARDS, transactionBuilder.type)
  }

  fun sendRevenueEvent() {
    analytics.sendRevenueEvent(formatter.scaleFiat(appcoinsRewardsBuyInteract.convertToFiat(
        transactionBuilder.amount()
            .toDouble(), FacebookEventLogger.EVENT_REVENUE_CURRENCY)
        .blockingGet()
        .amount)
        .toString())
  }

  fun sendPaymentSuccessEvent() {
    analytics.sendPaymentSuccessEvent(packageName, transactionBuilder.skuId,
        transactionBuilder.amount()
            .toString(), BillingAnalytics.PAYMENT_METHOD_REWARDS, transactionBuilder.type)
  }

  private fun sendPaymentErrorEvent(transaction: RewardPayment) {
    val status = transaction.status
    if (isErrorStatus(status)) {
      if (transaction.errorCode == null && transaction.errorMessage == null) {
        analytics.sendPaymentErrorEvent(packageName, transactionBuilder.skuId,
            transactionBuilder.amount()
                .toString(), BillingAnalytics.PAYMENT_METHOD_REWARDS, transactionBuilder.type,
            status.toString())
      } else {
        analytics.sendPaymentErrorWithDetailsEvent(packageName, transactionBuilder.skuId,
            transactionBuilder.amount()
                .toString(), BillingAnalytics.PAYMENT_METHOD_REWARDS, transactionBuilder.type,
            transaction.errorCode.toString(), transaction.errorMessage.toString())
      }
    }
  }

  private fun isErrorStatus(status: Status): Boolean {
    return status === Status.ERROR || status === Status.NO_NETWORK ||
        status === Status.FORBIDDEN || status === Status.SUB_ALREADY_OWNED
  }

  private fun handleSupportClicks() {
    disposables.add(Observable.merge(view.getSupportIconClick(),
        view.getSupportLogoClick())
        .throttleFirst(50, TimeUnit.MILLISECONDS)
        .observeOn(viewScheduler)
        .flatMapCompletable { appcoinsRewardsBuyInteract.showSupport(gamificationLevel) }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun isManagedPaymentType(type: String): Boolean {
    return type == BillingSupportedType.INAPP.name || type == BillingSupportedType.INAPP_SUBSCRIPTION.name
  }
}