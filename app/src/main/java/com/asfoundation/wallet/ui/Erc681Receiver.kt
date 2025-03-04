package com.asfoundation.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.appcoins.wallet.bdsbilling.WalletService
import com.asf.wallet.R
import com.asfoundation.wallet.entity.TransactionBuilder
import com.asfoundation.wallet.ui.iab.IabActivity.Companion.PRODUCT_NAME
import com.asfoundation.wallet.ui.iab.IabActivity.Companion.newIntent
import com.asfoundation.wallet.ui.iab.InAppPurchaseInteractor
import com.asfoundation.wallet.ui.splash.SplashActivity
import com.asfoundation.wallet.util.TransferParser
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_iab_wallet_creation.*
import javax.inject.Inject

/**
 * Created by trinkes on 13/03/2018.
 */
class Erc681Receiver : BaseActivity(), Erc681ReceiverView {
  @Inject
  lateinit var walletService: WalletService

  @Inject
  lateinit var transferParser: TransferParser

  @Inject
  lateinit var inAppPurchaseInteractor: InAppPurchaseInteractor
  private lateinit var presenter: Erc681ReceiverPresenter


  companion object {
    const val REQUEST_CODE = 234
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_iab_wallet_creation)
    val productName = intent.extras!!.getString(PRODUCT_NAME, "")
    presenter =
        Erc681ReceiverPresenter(this, transferParser, inAppPurchaseInteractor, walletService,
            intent.dataString!!,
            AndroidSchedulers.mainThread(), CompositeDisposable(), productName)
    presenter.present(savedInstanceState)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int,
                                data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE) {
      setResult(resultCode, data)
      finish()
    }
  }

  override fun getCallingPackage(): String? {
    return super.getCallingPackage()
  }

  override fun startEipTransfer(transactionBuilder: TransactionBuilder, isBds: Boolean) {
    val intent: Intent = if (intent.data != null && intent.data.toString()
            .contains("/buy?")) {
      newIntent(this, intent, transactionBuilder, isBds, transactionBuilder.payload)
    } else {
      SendActivity.newIntent(this, intent)
    }
    startActivityForResult(intent, REQUEST_CODE)
  }

  override fun startApp(throwable: Throwable) {
    throwable.printStackTrace()
    startActivity(SplashActivity.newIntent(this))
    finish()
  }

  override fun endAnimation() {
    create_wallet_animation?.visibility = View.INVISIBLE
    create_wallet_text?.visibility = View.INVISIBLE
    create_wallet_card?.visibility = View.INVISIBLE
    create_wallet_animation?.removeAllAnimatorListeners()
    create_wallet_animation?.removeAllUpdateListeners()
    create_wallet_animation?.removeAllLottieOnCompositionLoadedListener()
  }

  override fun showLoadingAnimation() {
    create_wallet_animation?.visibility = View.VISIBLE
    create_wallet_card?.visibility = View.VISIBLE
    create_wallet_text?.visibility = View.VISIBLE
    create_wallet_animation?.playAnimation()
  }

  override fun onPause() {
    presenter.pause()
    super.onPause()
  }
}