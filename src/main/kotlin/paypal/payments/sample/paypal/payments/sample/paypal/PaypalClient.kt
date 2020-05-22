package paypal.payments.sample.paypal.payments.sample.paypal

import com.paypal.core.PayPalEnvironment
import com.paypal.core.PayPalHttpClient

open class PaypalClient {

    /**
     * PayPal access credentials
     * For Sandbox use @PayPalEnvironment.Sandbox()
     * For Production use @PayPalEnvironment.Live()
     */
    private val env: PayPalEnvironment = PayPalEnvironment.Sandbox(clientId, clientSecret)

    private val client = PayPalHttpClient(env)

    fun paypalClient(): PayPalHttpClient = client

    companion object {
        const val clientId = ""
        const val clientSecret = ""
    }
}