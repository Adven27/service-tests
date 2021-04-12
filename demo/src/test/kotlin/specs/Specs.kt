package specs

import com.example.demo.DemoApplication
import io.github.adven27.concordion.extensions.exam.core.AbstractSpecs
import io.github.adven27.concordion.extensions.exam.core.ExamExtension
import io.github.adven27.concordion.extensions.exam.db.DbPlugin
import io.github.adven27.concordion.extensions.exam.mq.MqPlugin
import io.github.adven27.concordion.extensions.exam.mq.rabbit.RabbitTester
import io.github.adven27.concordion.extensions.exam.ws.WsPlugin
import io.github.adven27.env.core.Environment
import io.github.adven27.env.db.postgresql.PostgreSqlContainerSystem
import io.github.adven27.env.mq.rabbit.RabbitContainerSystem
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class RestDeposit : Specs()
class RestWithdraw : Specs()
class RestBalance : Specs()
class MqApi : Specs()

open class Specs : AbstractSpecs() {
    companion object {
        private var SUT: ConfigurableApplicationContext? = null
        private val ENV: SomeEnvironment = SomeEnvironment().apply { up() }
    }

    override fun init() = ExamExtension(
        WsPlugin(ENV.sutPort()),
        DbPlugin(
            ENV.postgres().driver.value,
            ENV.postgres().jdbcUrl.value,
            ENV.postgres().username.value,
            ENV.postgres().password.value
        ),
        MqPlugin(
            mapOf(
                "in" to RabbitTester(exchange = "", routingKey = "in", queue = "in", ENV.rabbitPort()),
                "out" to RabbitTester(exchange = "", routingKey = "out", queue = "out", ENV.rabbitPort())
            )
        )
    )//.runOnlyExamplesWithPathsContains("Not first withdraw")

    override fun startSut() {
        SUT = SpringApplication(DemoApplication::class.java).apply { setAdditionalProfiles("qa") }.run()
    }

    override fun stopSut() = SUT!!.stop()
}

class SomeEnvironment : Environment(
    mapOf(
        "RABBIT" to RabbitContainerSystem(),
        "POSTGRES" to PostgreSqlContainerSystem(),
    )
) {
    fun rabbitPort() = find<RabbitContainerSystem>("RABBIT").config().port.value.toInt()
    fun postgres() = find<PostgreSqlContainerSystem>("POSTGRES").config()
    fun sutPort() = (if ("SPECS_ENV_FIXED".fromPropertyOrElse(false)) 8080 else findAvailableTcpPort()).also {
        System.setProperty("server.port", it.toString())
    }
}
