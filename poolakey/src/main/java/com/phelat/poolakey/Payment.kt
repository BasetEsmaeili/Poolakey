package com.phelat.poolakey

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.phelat.poolakey.callback.ConnectionCallback
import com.phelat.poolakey.callback.ConsumeCallback
import com.phelat.poolakey.callback.PurchaseCallback
import com.phelat.poolakey.callback.PurchaseIntentCallback
import com.phelat.poolakey.callback.PurchaseQueryCallback
import com.phelat.poolakey.config.PaymentConfiguration
import com.phelat.poolakey.mapper.RawDataToPurchaseInfo
import com.phelat.poolakey.request.PurchaseRequest
import com.phelat.poolakey.thread.BackgroundThread
import com.phelat.poolakey.thread.MainThread
import com.phelat.poolakey.thread.PoolakeyThread

class Payment(context: Context, config: PaymentConfiguration = PaymentConfiguration()) {

    private val rawDataToPurchaseInfo = RawDataToPurchaseInfo()

    private val backgroundThread = BackgroundThread()

    private val mainThread: PoolakeyThread<() -> Unit> = MainThread()

    private val connection = BillingConnection(
        context = context,
        paymentConfiguration = config,
        rawDataToPurchaseInfo = rawDataToPurchaseInfo,
        backgroundThread = backgroundThread,
        mainThread = mainThread
    )

    private val purchaseResultParser = PurchaseResultParser(rawDataToPurchaseInfo)

    fun connect(callback: ConnectionCallback.() -> Unit = {}): Connection {
        return connection.startConnection(callback)
    }

    fun purchaseItem(
        activity: Activity,
        request: PurchaseRequest,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        requestCode = request.requestCode
        connection.purchase(activity, request, PurchaseType.IN_APP, callback)
    }

    fun purchaseItem(
        fragment: Fragment,
        request: PurchaseRequest,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        requestCode = request.requestCode
        connection.purchase(fragment, request, PurchaseType.IN_APP, callback)
    }

    fun consumeItem(purchaseToken: String, callback: ConsumeCallback.() -> Unit) {
        connection.consume(purchaseToken, callback)
    }

    fun subscribeItem(
        activity: Activity,
        request: PurchaseRequest,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        requestCode = request.requestCode
        connection.purchase(activity, request, PurchaseType.SUBSCRIPTION, callback)
    }

    fun subscribeItem(
        fragment: Fragment,
        request: PurchaseRequest,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        requestCode = request.requestCode
        connection.purchase(fragment, request, PurchaseType.SUBSCRIPTION, callback)
    }

    fun getPurchasedItems(callback: PurchaseQueryCallback.() -> Unit) {
        connection.queryBoughtItems(PurchaseType.IN_APP, callback)
    }

    fun getSubscribedItems(callback: PurchaseQueryCallback.() -> Unit) {
        connection.queryBoughtItems(PurchaseType.SUBSCRIPTION, callback)
    }

    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        if (Payment.requestCode > -1 && Payment.requestCode == requestCode) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    purchaseResultParser.handleReceivedResult(data, purchaseCallback)
                }
                Activity.RESULT_CANCELED -> {
                    PurchaseCallback().apply(purchaseCallback)
                        .purchaseCanceled
                        .invoke()
                }
                else -> {
                    PurchaseCallback().apply(purchaseCallback)
                        .purchaseFailed
                        .invoke(IllegalStateException("Result code is not valid"))
                }
            }
        }
    }

    companion object {
        @Volatile
        private var requestCode: Int = -1
    }

}
