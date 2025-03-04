package com.asfoundation.wallet.ui.wallets

import com.asfoundation.wallet.ui.iab.FiatValue
import java.io.Serializable

data class WalletsModel(val totalBalance: FiatValue, val totalWallets: Int,
                        val currentWallet: WalletBalance,
                        val otherWallets: List<WalletBalance>) : Serializable
