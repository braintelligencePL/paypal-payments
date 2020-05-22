package paypal.payments.sample.paypal.payments.sample.paypal

import paypal.payments.sample.paypal.payments.sample.paypal.model.PaypalHttpException
import com.google.gson.Gson
import com.paypal.api.payments.Event
import com.paypal.base.rest.JSONFormatter.GSON
import com.paypal.http.exceptions.HttpException
import com.paypal.orders.AmountWithBreakdown
import com.paypal.orders.ApplicationContext
import com.paypal.orders.OrderRequest
import com.paypal.orders.OrdersCaptureRequest
import com.paypal.orders.OrdersCreateRequest
import com.paypal.orders.OrdersGetRequest
import com.paypal.orders.PurchaseUnitRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import paypal.payments.sample.paypal.payments.sample.paypal.model.OrderStatusCheckResponse
import paypal.payments.sample.paypal.payments.sample.paypal.model.PaypalIntentResponse
import java.net.URI

@RestController
@RequestMapping
class AppEndpoint : PaypalClient() {

    @GetMapping("/paypal")
    fun createIntent(): PaypalIntentResponse {

        val deserializedIntent = paypalClient()
                .execute(prepareRequest())
                .result()

        val approveLink = deserializedIntent.links().first { it.rel() == "approve" }

        return PaypalIntentResponse(
                checkoutApprove = URI.create(approveLink.href())
        )
    }

    @PostMapping("/paypal/webhook/checkout-order-approved")
    fun execute(
            @RequestBody event: String
    ): HttpStatus {

        println(" >> checkout-order-approved <<")

        println("$event")

        println()
        println(GSON.fromJson(event, Event::class.java))

        val id = (GSON.fromJson(event, Event::class.java).resource as Map<String, String>)["id"]

        paypalClient().execute(OrdersGetRequest(id)).result().let {
            OrderStatusCheckResponse(
                    id = it.id(),
                    status = it.status()
            ).also { response ->
                println("BEFORE EXECUTION >> OrderStatusCheck after approval: == $response")
            }
        }

        try {
            paypalClient().execute(prepareCaptureRequest(id!!))
        } catch (ex: HttpException) {
            val exception = Gson().fromJson(ex.message, PaypalHttpException::class.java)

            if (exception.name == "UNPROCESSABLE_ENTITY" && exception.details.first().issue == "INSTRUMENT_DECLINED") {
                println("${exception.details.first().issue} : ${exception.details.first().description} ")
            } else {
                println("Not handled error occured = $exception")
            }
        }

        paypalClient().execute(OrdersGetRequest(id)).result().let {
            OrderStatusCheckResponse(
                    id = it.id(),
                    status = it.status()
            ).also { response ->
                println("AFTER EXECUTION >> OrderStatusCheck after approval: == $response")
            }
        }

        return HttpStatus.ACCEPTED
    }

    @PostMapping("/paypal/webhook/payment-capture-completed")
    fun execute2(
            @RequestBody event: String
    ): HttpStatus {

        println(" >> payment-capture-completed <<")

        println("$event")

        println()
        println(GSON.fromJson(event, Event::class.java))

        return HttpStatus.ACCEPTED
    }

    @GetMapping("/paypal/orders/{orderId}")
    fun checkOrder(
            @PathVariable orderId: String
    ): OrderStatusCheckResponse {
        val response = paypalClient().execute(OrdersGetRequest(orderId)).result()
        return OrderStatusCheckResponse(
                id = response.id(),
                status = response.status()
        )
    }

    fun prepareCaptureRequest(id: String): OrdersCaptureRequest? {
        return OrdersCaptureRequest(id)
    }

    fun prepareRequest(): OrdersCreateRequest? {

        val request = OrderRequest()
                .checkoutPaymentIntent("CAPTURE")
                .applicationContext(ApplicationContext()
                        .brandName("Fourthwall")
                        .shippingPreference("NO_SHIPPING")
                        .returnUrl("https://fourthwall.com/return")
                        .cancelUrl("https://fourthwall.com/cancel")
                        .userAction("PAY_NOW")
                )
                .purchaseUnits(listOf(PurchaseUnitRequest()
                        .amountWithBreakdown(
                                AmountWithBreakdown()
                                        .currencyCode("USD")
                                        .value("107.55")))

                )

        return OrdersCreateRequest().requestBody(request)
    }
}
