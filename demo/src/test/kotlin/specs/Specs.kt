package specs

import com.example.demo.DemoApplication
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.github.adven27.concordion.extensions.exam.core.AbstractSpecs
import io.github.adven27.concordion.extensions.exam.core.ExamExtension
import io.github.adven27.concordion.extensions.exam.db.DbPlugin
import io.github.adven27.concordion.extensions.exam.mq.MqPlugin
import io.github.adven27.concordion.extensions.exam.mq.rabbit.RabbitTester
import io.github.adven27.concordion.extensions.exam.mq.rabbit.RabbitTester.ReceiveConfig
import io.github.adven27.concordion.extensions.exam.mq.rabbit.RabbitTester.SendConfig
import io.github.adven27.concordion.extensions.exam.ws.WsPlugin
import io.github.adven27.env.core.Environment
import io.github.adven27.env.db.postgresql.PostgreSqlContainerSystem
import io.github.adven27.env.mq.rabbit.RabbitContainerSystem
import io.github.adven27.env.wiremock.WiremockSystem
import org.concordion.api.extension.Extensions
import org.concordion.ext.runtotals.RunTotalsExtension
import org.concordion.ext.timing.TimerExtension
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class MqApi : Specs()

@Extensions(TimerExtension::class, RunTotalsExtension::class)
open class Specs : AbstractSpecs() {
    companion object {
        private var SUT: ConfigurableApplicationContext? = null

        @JvmStatic
        val ENV: SomeEnvironment = SomeEnvironment().apply { up() }
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
                "in" to RabbitTester(ENV.rabbitPort(), SendConfig("in"), ReceiveConfig("in")),
                "out" to RabbitTester(ENV.rabbitPort(), SendConfig("out"), ReceiveConfig("out")),
            )
        )
    ).runOnlyExamplesWithPathsContains(*System.getProperty("SPECS_RUN_ONLY", "/").split(",").toTypedArray())

    override fun startSut() {
        SUT = SpringApplication(DemoApplication::class.java).apply { setAdditionalProfiles("qa") }.run()
    }

    override fun stopSut() = SUT!!.stop()
}

class SomeEnvironment : Environment(
    mapOf(
        "RABBIT" to RabbitContainerSystem(),
        "POSTGRES" to PostgreSqlContainerSystem(),
        "SES" to WiremockSystem(afterStart = { stubFor(post(urlPathEqualTo("/hook")).willReturn(aResponse())) })
    )
) {
    fun ses() = WireMock(System.getProperty("env.wiremock.port").toInt())
    fun rabbitPort() = find<RabbitContainerSystem>("RABBIT").config().port.value.toInt()
    fun postgres() = find<PostgreSqlContainerSystem>("POSTGRES").config()
    fun sutPort() = (if ("SPECS_ENV_FIXED".fromPropertyOrElse(false)) 8080 else findAvailableTcpPort()).also {
        System.setProperty("server.port", it.toString())
    }
}
