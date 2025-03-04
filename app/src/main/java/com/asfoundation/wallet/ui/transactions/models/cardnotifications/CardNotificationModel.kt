package com.asfoundation.wallet.ui.transactions.models.cardnotifications

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.airbnb.lottie.LottieAnimationView
import com.asf.wallet.R
import com.asfoundation.wallet.GlideApp
import com.asfoundation.wallet.interact.UpdateNotification
import com.asfoundation.wallet.promotions.PromotionNotification
import com.asfoundation.wallet.referrals.CardNotification
import com.asfoundation.wallet.referrals.ReferralNotification
import com.asfoundation.wallet.ui.common.BaseViewHolder
import com.asfoundation.wallet.ui.widget.holder.CardNotificationAction
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.item_card_notification.view.*

@EpoxyModelClass
abstract class CardNotificationModel :
    EpoxyModelWithHolder<CardNotificationModel.NotificationHolder>() {

  @EpoxyAttribute
  var cardNotification: CardNotification? = null

  @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
  var clickListener: ((CardNotification, CardNotificationAction) -> Unit)? = null

  override fun getDefaultLayout(): Int = R.layout.item_card_notification

  override fun bind(holder: NotificationHolder) {
    cardNotification?.let { notification ->
      setTitle(holder, notification)
      setBody(holder, notification)
      setImage(holder, notification)
      setButtonActions(holder, notification)
    }
  }

  private fun setTitle(holder: NotificationHolder,
                       notification: CardNotification) {
    when (notification) {
      is ReferralNotification -> holder.title.text =
          holder.itemView.context.getString(notification.title,
              notification.symbol + notification.pendingAmount)
      is PromotionNotification -> holder.title.text = notification.noResTitle
      else -> notification.title?.let {
        holder.title.text = holder.itemView.context.getString(it)
      }
    }
  }

  private fun setBody(holder: NotificationHolder,
                      notification: CardNotification) {
    if (notification is PromotionNotification) {
      holder.description.text = notification.noResBody
    } else {
      notification.body?.let { holder.description.setText(it) }
    }
  }

  private fun setImage(holder: NotificationHolder,
                       notification: CardNotification) {
    when (notification) {
      is UpdateNotification -> {
        holder.animation.setAnimation(notification.animation)
        holder.image.visibility = View.INVISIBLE
        holder.animation.visibility = View.VISIBLE
      }
      is PromotionNotification -> {
        holder.image.visibility = View.VISIBLE
        holder.animation.visibility = View.INVISIBLE
        GlideApp.with(holder.itemView.context)
            .load(notification.noResIcon)
            .error(R.drawable.ic_promotions_default)
            .circleCrop()
            .into(holder.itemView.notification_image)
      }
      else -> {
        holder.image.visibility = View.VISIBLE
        holder.animation.visibility = View.INVISIBLE
        notification.icon?.let {
          holder.image.setImageResource(it)
          holder.image.visibility = View.VISIBLE
        }
      }
    }
  }

  private fun setButtonActions(holder: NotificationHolder,
                               notification: CardNotification) {
    holder.dismissButton.setOnClickListener {
      clickListener?.invoke(notification, CardNotificationAction.DISMISS)
    }
    if (notification is PromotionNotification) {
      holder.positiveButton.visibility = View.GONE
      holder.itemView.setOnClickListener {
        clickListener?.invoke(notification, notification.positiveAction)
      }
      holder.positiveButton.setOnClickListener { }
    } else {
      notification.positiveButtonText?.let {
        holder.itemView.notification_positive_button.setText(it)
      }
      holder.itemView.setOnClickListener { }
      holder.positiveButton.setOnClickListener {
        clickListener?.invoke(notification, notification.positiveAction)
      }
    }
  }

  class NotificationHolder : BaseViewHolder() {
    val image by bind<ImageView>(R.id.notification_image)
    val animation by bind<LottieAnimationView>(R.id.notification_animation)
    val title by bind<TextView>(R.id.notification_title)
    val description by bind<TextView>(R.id.notification_description)
    val positiveButton by bind<MaterialButton>(R.id.notification_positive_button)
    val dismissButton by bind<MaterialButton>(R.id.notification_dismiss_button)
  }

}