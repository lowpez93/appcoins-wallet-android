package com.asfoundation.wallet.my_wallets.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.asf.wallet.R
import com.asf.wallet.databinding.FragmentMyWalletsBinding
import com.asfoundation.wallet.base.SingleStateFragment
import com.asfoundation.wallet.my_wallets.main.list.WalletsController
import com.asfoundation.wallet.my_wallets.main.list.WalletsListEvent
import com.asfoundation.wallet.ui.MyAddressActivity
import com.asfoundation.wallet.ui.balance.BalanceVerificationStatus
import com.asfoundation.wallet.ui.balance.TokenBalance
import com.asfoundation.wallet.ui.iab.FiatValue
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.util.WalletCurrency
import com.asfoundation.wallet.util.getDrawableURI
import com.asfoundation.wallet.util.safeLet
import com.asfoundation.wallet.viewmodel.BasePageViewFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import java.math.BigDecimal
import javax.inject.Inject

class MyWalletsFragment : BasePageViewFragment(),
    SingleStateFragment<MyWalletsState, MyWalletsSideEffect> {

  @Inject
  lateinit var viewModelFactory: MyWalletsViewModelFactory

  @Inject
  lateinit var formatter: CurrencyFormatUtils

  @Inject
  lateinit var navigator: MyWalletsNavigator

  private val viewModel: MyWalletsViewModel by viewModels { viewModelFactory }

  private var binding: FragmentMyWalletsBinding? = null
  private val views get() = binding!!

  private lateinit var walletsController: WalletsController
  val epoxyVisibilityTracker = EpoxyVisibilityTracker()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    binding = FragmentMyWalletsBinding.inflate(inflater, container, false)
    return views.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeView()
    setListeners()
    viewModel.collectStateAndEvents(lifecycle, viewLifecycleOwner.lifecycleScope)
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshData()
  }

  override fun onDestroyView() {
    epoxyVisibilityTracker.detach(views.otherWalletsRecyclerView)
    super.onDestroyView()
    binding = null
  }

  private fun initializeView() {
    epoxyVisibilityTracker.attach(views.otherWalletsRecyclerView)
    walletsController = WalletsController()
    walletsController.walletClickListener = { click ->
      when (click) {
        WalletsListEvent.CreateNewWalletClick -> navigator.navigateToCreateNewWallet()
        is WalletsListEvent.OtherWalletClick -> navigator.navigateToChangeActiveWallet(
            click.walletBalance)
        is WalletsListEvent.CopyWalletClick -> setAddressToClipBoard(click.walletAddress)
        is WalletsListEvent.ShareWalletClick -> showShare(click.walletAddress)
        WalletsListEvent.BackupClick -> {
          viewModel.state.walletsAsync()
              ?.let { walletsModel ->
                navigator.navigateToBackupWallet(walletsModel.currentWallet.walletAddress)
              }
        }
        WalletsListEvent.VerifyWalletClick -> navigator.navigateToVerifyPicker()
        WalletsListEvent.VerifyInsertCodeClick -> navigator.navigateToVerifyCreditCard()
        is WalletsListEvent.QrCodeClick -> navigator.navigateToQrCode(click.view)
        is WalletsListEvent.TokenClick -> {
          when (click.token) {
            WalletsListEvent.TokenClick.Token.APPC -> {
              navigateToTokenInfo(R.string.appc_token_name, R.string.p2p_send_currency_appc,
                  R.drawable.ic_appc, R.string.balance_appcoins_body, false)
            }
            WalletsListEvent.TokenClick.Token.APPC_C -> {
              navigateToTokenInfo(R.string.appc_credits_token_name,
                  R.string.p2p_send_currency_appc_c,
                  R.drawable.ic_appc_c_token, R.string.balance_appccreditos_body, true)
            }
            WalletsListEvent.TokenClick.Token.ETH -> {
              navigateToTokenInfo(R.string.ethereum_token_name, R.string.p2p_send_currency_eth,
                  R.drawable.ic_eth_token, R.string.balance_ethereum_body, false)
            }
          }
        }
        is WalletsListEvent.ChangedBalanceVisibility -> {
          if (click.balanceVisible) {
            binding?.titleSwitcher?.setInAnimation(requireContext(), R.anim.slide_in_up)
            binding?.titleSwitcher?.setOutAnimation(requireContext(), R.anim.slide_out_down)
            binding?.titleSwitcher?.setText(getString(R.string.wallets_active_wallet_title))
          } else {
            viewModel.state.balanceAsync()
                ?.let { balance ->
                  binding?.titleSwitcher?.setInAnimation(requireContext(), R.anim.slide_in_down)
                  binding?.titleSwitcher?.setOutAnimation(requireContext(), R.anim.slide_out_up)
                  binding?.titleSwitcher?.setText(getFiatBalanceText(balance.overallFiat))
                }
          }
        }
      }
    }
    views.otherWalletsRecyclerView.setController(walletsController)
    epoxyVisibilityTracker.requestVisibilityCheck()
  }

  private fun navigateToTokenInfo(tokenNameRes: Int, tokenSymbolRes: Int, tokenImageRes: Int,
                                  tokenDescriptionRes: Int, showTopUp: Boolean) {
    val title = "${getString(tokenNameRes)} (${getString(tokenSymbolRes)})"
    val image = requireContext().getDrawableURI(tokenImageRes)
    val description = getString(tokenDescriptionRes)
    navigator.navigateToTokenInfo(title, image, description, showTopUp)
  }

  private fun setListeners() {
    views.actionButtonMore.setOnClickListener { navigateToMore() }
    views.actionButtonNfts.setOnClickListener { navigator.navigateToNfts()}
  }

  override fun onStateChanged(state: MyWalletsState) {
    walletsController.setData(state.walletsAsync, state.walletVerifiedAsync,
        state.balanceAsync, state.backedUpOnceAsync)
  }

  override fun onSideEffect(sideEffect: MyWalletsSideEffect) = Unit

  private fun getFiatBalanceText(balance: FiatValue): String {
    var overallBalance = "-1"
    if (balance.amount.compareTo(BigDecimal("-1")) == 1) {
      overallBalance = formatter.formatCurrency(balance.amount)
    }
    if (overallBalance != "-1") {
      return balance.symbol + overallBalance
    }
    return overallBalance
  }

  private fun getTokenValueText(balance: TokenBalance, tokenCurrency: WalletCurrency): String {
    var tokenBalance = "-1"
    if (balance.token.amount.compareTo(BigDecimal("-1")) == 1) {
      tokenBalance = formatter.formatCurrency(balance.token.amount, tokenCurrency)
    }
    return tokenBalance
  }

  private fun navigateToMore() {
    safeLet(viewModel.state.balanceAsync(),
        viewModel.state.walletsAsync(),
        viewModel.state.walletVerifiedAsync()) { balanceScreenModel, walletsModel, verifyModel ->
      val verifyStatus = verifyModel.status ?: verifyModel.cachedStatus
      val verified = verifyStatus == BalanceVerificationStatus.VERIFIED
      val overallFiatValue = getFiatBalanceText(balanceScreenModel.overallFiat)
      val appcoinsValue = "${
        getTokenValueText(balanceScreenModel.appcBalance, WalletCurrency.APPCOINS)
      } ${balanceScreenModel.appcBalance.token.symbol}"
      val creditsValue = "${
        getTokenValueText(balanceScreenModel.creditsBalance, WalletCurrency.CREDITS)
      } ${balanceScreenModel.creditsBalance.token.symbol}"
      val ethValue = "${
        getTokenValueText(balanceScreenModel.ethBalance, WalletCurrency.ETHEREUM)
      } ${balanceScreenModel.ethBalance.token.symbol}"
      navigator.navigateToMore(walletsModel.currentWallet.walletAddress, overallFiatValue,
          appcoinsValue, creditsValue, ethValue, verified, walletsModel.otherWallets.isNotEmpty())
    }
  }

  fun setAddressToClipBoard(walletAddress: String) {
    val clipboard =
        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText(
        MyAddressActivity.KEY_ADDRESS, walletAddress)
    clipboard?.setPrimaryClip(clip)
    val bottomNavView: BottomNavigationView = requireActivity().findViewById(R.id.bottom_nav)!!

    Snackbar.make(bottomNavView, R.string.wallets_address_copied_body, Snackbar.LENGTH_SHORT)
        .apply {
          anchorView = bottomNavView
        }
        .show()
  }

  fun showShare(walletAddress: String) {
    ShareCompat.IntentBuilder.from(requireActivity())
        .setText(walletAddress)
        .setType("text/plain")
        .setChooserTitle(resources.getString(R.string.share_via))
        .startChooser()
  }
}