package com.asfoundation.wallet.eskills.withdraw.repository

import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface WithdrawApi {
  @GET("transaction/wallet/withdraw/credits")
  fun getAvailableAmount(
      @Header("authorization") authorization: String
  ): Single<WithdrawAvailableAmountResult>

  @POST("transaction/wallet/withdraw/credits")
  fun withdrawAppcCredits(
      @Header("authorization") authorization: String,
      @Body withdrawBody: WithdrawBody
  ): Completable
}
