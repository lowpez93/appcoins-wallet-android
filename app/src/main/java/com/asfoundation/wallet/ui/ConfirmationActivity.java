package com.asfoundation.wallet.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProviders;
import com.asf.wallet.R;
import com.asfoundation.wallet.C;
import com.asfoundation.wallet.entity.ErrorEnvelope;
import com.asfoundation.wallet.entity.PendingTransaction;
import com.asfoundation.wallet.entity.TransactionBuilder;
import com.asfoundation.wallet.util.BalanceUtils;
import com.asfoundation.wallet.util.CurrencyFormatUtils;
import com.asfoundation.wallet.util.WalletCurrency;
import com.asfoundation.wallet.viewmodel.ConfirmationViewModel;
import com.asfoundation.wallet.viewmodel.ConfirmationViewModelFactory;
import com.asfoundation.wallet.viewmodel.GasSettingsViewModel;
import dagger.android.AndroidInjection;
import java.math.BigDecimal;
import javax.inject.Inject;

import static com.asfoundation.wallet.C.EXTRA_GAS_SETTINGS;
import static com.asfoundation.wallet.C.EXTRA_TRANSACTION_BUILDER;
import static com.asfoundation.wallet.C.GWEI_UNIT;

public class ConfirmationActivity extends BaseActivity {
  private static final String TAG = ConfirmationActivity.class.getSimpleName();

  AlertDialog dialog;
  @Inject ConfirmationViewModelFactory confirmationViewModelFactory;
  CurrencyFormatUtils currencyFormatUtils;
  ConfirmationViewModel viewModel;
  private TextView fromAddressText;
  private TextView toAddressText;
  private TextView valueText;
  private TextView gasPriceText;
  private TextView gasLimitText;
  private TextView networkFeeText;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    AndroidInjection.inject(this);

    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_confirm);
    toolbar();
    currencyFormatUtils = CurrencyFormatUtils.Companion.create();
    fromAddressText = findViewById(R.id.text_from);
    toAddressText = findViewById(R.id.text_to);
    valueText = findViewById(R.id.text_value);
    gasPriceText = findViewById(R.id.text_gas_price);
    gasLimitText = findViewById(R.id.text_gas_limit);
    networkFeeText = findViewById(R.id.text_network_fee);
    findViewById(R.id.send_button).setOnClickListener(view -> onSend());

    viewModel = ViewModelProviders.of(this, confirmationViewModelFactory)
        .get(ConfirmationViewModel.class);
    viewModel.transactionBuilder()
        .observe(this, this::onTransactionBuilder);
    viewModel.transactionHash()
        .observe(this, this::onTransaction);

    viewModel.progress()
        .observe(this, this::onProgress);
    viewModel.error()
        .observe(this, this::onError);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_edit) {
      viewModel.openGasSettings(ConfirmationActivity.this);
    }
    return super.onOptionsItemSelected(item);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == GasSettingsViewModel.SET_GAS_SETTINGS) {
      if (resultCode == RESULT_OK) {
        viewModel.setGasSettings(intent.getParcelableExtra(EXTRA_GAS_SETTINGS));
      }
    }
  }

  private void onTransactionBuilder(TransactionBuilder transactionBuilder) {
    fromAddressText.setText(transactionBuilder.fromAddress());
    toAddressText.setText(transactionBuilder.toAddress());

    String value = "-" + currencyFormatUtils.formatTransferCurrency(transactionBuilder.amount(),
        WalletCurrency.ETHEREUM);
    String symbol = transactionBuilder.symbol();
    int smallTitleSize = (int) getResources().getDimension(R.dimen.small_text);
    int color = getResources().getColor(R.color.color_grey_9e);
    valueText.setText(BalanceUtils.formatBalance(value, symbol, smallTitleSize, color));
    BigDecimal gasPrice = transactionBuilder.gasSettings().gasPrice;
    BigDecimal gasLimit = transactionBuilder.gasSettings().gasLimit;
    String formattedGasPrice = getString(R.string.gas_price_value,
        currencyFormatUtils.formatTransferCurrency(BalanceUtils.weiToGweiBigDecimal(gasPrice),
            WalletCurrency.ETHEREUM), GWEI_UNIT);
    gasPriceText.setText(formattedGasPrice);
    gasLimitText.setText(transactionBuilder.gasSettings().gasLimit.toPlainString());

    String networkFee = currencyFormatUtils.formatTransferCurrency(
        BalanceUtils.weiToEth(gasPrice.multiply(gasLimit)), WalletCurrency.ETHEREUM)
        + " "
        + C.ETH_SYMBOL;
    networkFeeText.setText(networkFee);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.confirmation_menu, menu);

    return super.onCreateOptionsMenu(menu);
  }

  private void onProgress(boolean shouldShowProgress) {
    if (shouldShowProgress) {
      hideDialog();
      ProgressBar progressBar = new ProgressBar(this);
      progressBar.setIndeterminateDrawable(
          ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_progress, null));
      dialog = new AlertDialog.Builder(this).setTitle(R.string.title_dialog_sending)
          .setView(progressBar)
          .setCancelable(false)
          .create();
      dialog.show();
    }
  }

  private void hideDialog() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  private void onSend() {
    viewModel.send();
  }

  private void onTransaction(PendingTransaction transaction) {
    Log.d(TAG, "onTransaction() called with: transaction = [" + transaction + "]");
    if (!transaction.isPending()) {
      viewModel.progressFinished();
      hideDialog();
      dialog = new AlertDialog.Builder(this).setTitle(R.string.transaction_succeeded)
          .setMessage(transaction.getHash())
          .setPositiveButton(R.string.button_ok,
              (dialog1, id) -> successFinish(transaction.getHash()))
          .setNeutralButton(R.string.copy, (dialog1, id) -> {
            ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction transaction", transaction.getHash());
            clipboard.setPrimaryClip(clip);
            successFinish(transaction.getHash());
          })
          .create();
      dialog.show();
    }
  }

  private void successFinish(String hash) {
    Intent intent = new Intent();
    intent.putExtra("transaction_hash", hash);
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  private void onError(ErrorEnvelope error) {
    hideDialog();
    AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.error_transaction_failed)
        .setMessage(error.message)
        .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
          // Do nothing
        })
        .create();
    dialog.show();
  }

  @Override protected void onResume() {
    super.onResume();

    viewModel.init(getIntent().getParcelableExtra(EXTRA_TRANSACTION_BUILDER));
  }
}
