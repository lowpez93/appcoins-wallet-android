package com.asfoundation.wallet.ui.settings.wallets.bottomsheet

import androidx.fragment.app.Fragment
import com.asfoundation.wallet.billing.analytics.WalletsEventSender
import com.asfoundation.wallet.ui.settings.wallets.bottomsheet.SettingsWalletsBottomSheetFragment.Companion.WALLET_MODEL_KEY
import com.asfoundation.wallet.ui.wallets.WalletsModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import io.reactivex.disposables.CompositeDisposable

@InstallIn(FragmentComponent::class)
@Module
class SettingsWalletsBottomSheetModule {

  @Provides
  fun providesSettingsWalletsBottomSheetPresenter(fragment: SettingsWalletsBottomSheetFragment,
                                                  navigator: SettingsWalletsBottomSheetNavigator,
                                                  walletsEventSender: WalletsEventSender,
                                                  data: SettingsWalletsBottomSheetData): SettingsWalletsBottomSheetPresenter {
    return SettingsWalletsBottomSheetPresenter(fragment as SettingsWalletsBottomSheetView,
        navigator, CompositeDisposable(), walletsEventSender, data)
  }

  @Provides
  fun providesSettingsWalletsBottomSheetData(
      fragment: SettingsWalletsBottomSheetFragment): SettingsWalletsBottomSheetData {
    fragment.arguments!!.apply {
      return SettingsWalletsBottomSheetData(getSerializable(WALLET_MODEL_KEY) as WalletsModel)
    }
  }

  @Provides
  fun providesSettingsWalletsBottomSheetNavigator(
      fragment: SettingsWalletsBottomSheetFragment): SettingsWalletsBottomSheetNavigator {
    return SettingsWalletsBottomSheetNavigator(fragment.requireFragmentManager(), fragment)
  }

  @Provides
  fun providesFragment(fragment: Fragment): SettingsWalletsBottomSheetFragment {
    return fragment as SettingsWalletsBottomSheetFragment
  }
}