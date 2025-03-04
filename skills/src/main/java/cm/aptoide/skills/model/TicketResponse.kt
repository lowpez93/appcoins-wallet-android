package cm.aptoide.skills.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TicketResponse(
    @SerializedName("ts")
    val timestamp: String,

    @SerializedName("ticket_id")
    val ticketId: String,

    @SerializedName("ticket_status")
    val ticketStatus: TicketStatus,

    @SerializedName("wallet_address")
    val walletAddress: String,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("callback_url")
    val callbackUrl: String,

    @SerializedName("ticket_price")
    val ticketPrice: BigDecimal,

    @SerializedName("price_currency")
    val priceCurrency: String,

    @SerializedName("product_token")
    val productToken: String,

    @SerializedName("room_metadata")
    val roomMetadata: Map<String, String>,

    @SerializedName("payment_transaction")
    val paymentTransaction: String,

    @SerializedName("room_id")
    val roomId: String?
)