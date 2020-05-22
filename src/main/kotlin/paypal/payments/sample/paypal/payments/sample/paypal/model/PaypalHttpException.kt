package paypal.payments.sample.paypal.payments.sample.paypal.model

data class PaypalHttpException(
        val name: String,
        val details: List<Details>,
        val message: String,
        val debug_id: String,
        val links: List<Links>
)

data class Links(
        val href: String,
        val rel: String,
        val method: String
)

data class Details(
        val issue: String,
        val description: String
)