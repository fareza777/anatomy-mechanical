package studio.anatomy.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import studio.anatomy.app.ads.Ads
import studio.anatomy.app.data.AnatomyPrefs

/**
 * One-time "remove ads" purchase. The product is a non-consumable in-app product
 * (product id `remove_ads`, $4.99) configured in Play Console. The entitlement is
 * acknowledged with Play and cached locally so the atlas opens ad-free offline.
 */
object RemoveAds {
    const val PRODUCT_ID = "remove_ads"
    private const val TAG = "RemoveAds"

    var owned by mutableStateOf(false)
        private set
    var price by mutableStateOf("\$4.99")
        private set
    var busy by mutableStateOf(false)
        private set

    private var prefs: AnatomyPrefs? = null
    private var client: BillingClient? = null
    private var product: ProductDetails? = null
    private var messageSink: ((String) -> Unit)? = null

    fun init(context: Context, prefs: AnatomyPrefs) {
        this.prefs = prefs
        owned = prefs.removeAds
        Ads.purchased = owned
        if (client != null) return
        val billing = BillingClient.newBuilder(context.applicationContext)
            .setListener(PurchasesUpdatedListener { result, purchases -> onPurchasesUpdated(result, purchases) })
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .build()
        client = billing
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    restore()
                } else {
                    Log.w(TAG, "Billing unavailable: ${result.responseCode} ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun queryProduct() {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        billing.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Product query failed: ${result.responseCode} ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            val item = details.firstOrNull() ?: return@queryProductDetailsAsync
            product = item
            item.oneTimePurchaseOfferDetails?.formattedPrice?.let { price = it }
        }
    }

    private fun restore() {
        val billing = client ?: return
        billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { purchase ->
                if (purchase.products.contains(PRODUCT_ID)) grant(purchase, silent = true)
            }
        }
    }

    fun purchase(activity: Activity, onMessage: (String) -> Unit) {
        messageSink = onMessage
        val billing = client
        val details = product
        if (billing == null || details == null) {
            onMessage("Purchases are not available yet. Try again in a moment.")
            return
        }
        if (owned) {
            onMessage("Ads are already removed on this account.")
            return
        }
        busy = true
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        val result = billing.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = false
            onMessage("Google Play billing is not available on this device.")
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID)) grant(purchase)
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                busy = false
            }
            else -> {
                busy = false
                Log.w(TAG, "Purchase failed: ${result.responseCode} ${result.debugMessage}")
                messageSink?.invoke("Purchase failed. Nothing was charged.")
            }
        }
    }

    private fun grant(purchase: Purchase, silent: Boolean = false) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client?.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.responseCode}")
                }
            }
        }
        prefs?.removeAds = true
        Ads.purchased = true
        owned = true
        busy = false
        if (!silent) messageSink?.invoke("Ads removed. Thank you.")
    }
}
