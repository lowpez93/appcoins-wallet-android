package com.asfoundation.wallet.verification.usecases

import com.appcoins.wallet.bdsbilling.WalletService
import com.appcoins.wallet.billing.adyen.AdyenPaymentRepository
import com.appcoins.wallet.billing.adyen.PaymentInfoModel
import com.appcoins.wallet.billing.adyen.VerificationInfoResponse
import com.asfoundation.wallet.base.RxSchedulers
import com.asfoundation.wallet.billing.adyen.AdyenPaymentInteractor
import com.asfoundation.wallet.verification.repository.VerificationRepository
import com.asfoundation.wallet.verification.ui.credit_card.intro.VerificationInfoModel
import com.asfoundation.wallet.verification.ui.credit_card.intro.VerificationIntroModel
import io.reactivex.Single

class GetVerificationInfoUseCase(
    private val walletService: WalletService,
    private val verificationRepository: VerificationRepository,
    private val adyenPaymentInteractor: AdyenPaymentInteractor,
    private val rxSchedulers: RxSchedulers
) {

  operator fun invoke(method: AdyenPaymentRepository.Methods): Single<VerificationIntroModel> {
    return walletService.getAndSignCurrentWalletAddress()
        .flatMap { walletModel ->
          verificationRepository.getVerificationInfo(walletModel.address, walletModel.signedAddress)
        }
        .flatMap { verificationInfo ->
          adyenPaymentInteractor.loadPaymentInfo(method, verificationInfo.value,
              verificationInfo.currency)
              .map { paymentInfo -> mapToVerificationIntroModel(verificationInfo, paymentInfo) }
        }
        .subscribeOn(rxSchedulers.io)
  }

  private fun mapToVerificationIntroModel(
      verificationInfo: VerificationInfoResponse,
      paymentInfoModel: PaymentInfoModel): VerificationIntroModel {
    val verificationInfoModel =
        VerificationInfoModel(verificationInfo.currency, verificationInfo.symbol,
            verificationInfo.value,
            verificationInfo.digits, verificationInfo.format, verificationInfo.period)
    return VerificationIntroModel(verificationInfoModel, paymentInfoModel)
  }
}