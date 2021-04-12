package com.example.demo

import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.beans.ConstructorProperties

@EnableRabbit
@Configuration
class MqConfig {
    @Bean
    fun inQueue() = Queue("in")

    @Bean
    fun outQueue() = Queue("out")

    @Bean
    fun converter() = Jackson2JsonMessageConverter()
}

@Component
class DepositListener(private val walletService: WalletService) {

    @RabbitListener(queues = ["in"])
    fun receive(event: DepositEvent) {
        walletService.deposit(event.user, event.amount.toBigDecimal(), event.currency)
    }

    data class DepositEvent @ConstructorProperties("user", "amount", "currency") constructor(
        val user: Long, val amount: String, val currency: String
    )
}
