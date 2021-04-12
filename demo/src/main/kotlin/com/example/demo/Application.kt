package com.example.demo

import liquibase.pro.packaged.it
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Service
class WalletService(private val repo: AccountRepo, private val rabbitTemplate: RabbitTemplate) {
    fun deposit(user: Long, amount: BigDecimal, currency: String): Account {
        requireNotNegative(amount)
        return save(
            repo.findById(AccountId(user, currency))
                .map { it.deposit(amount) }
                .orElseGet { createAccount(user, currency, amount) }
        )
    }

    private fun createAccount(user: Long, currency: String, amount: BigDecimal) = Account(user, currency, amount).also {
        rabbitTemplate.send(
            "out",
            MessageBuilder //language=xml
                .withBody("<event><type>USER_CREATED</type><details><id>$user</id></details></event>".toByteArray())
                .build()
        )
    }

    fun withdraw(user: Long, amount: BigDecimal, currency: String): Account {
        requireNotNegative(amount)
        return save(
            repo.findById(AccountId(user, currency))
                .map { it.withdraw(amount) }
                .filter { it.balance >= ZERO }
                .orElseThrow { InsufficientFunds() }
        )
    }

    fun balance(user: Long) = repo.findByAccountIdUserId(user).map { it.currency to it.balance }.toMap()

    private fun save(account: Account): Account = try {
        repo.save(account)
    } catch (e: ObjectOptimisticLockingFailureException) {
        throw StaleState()
    } catch (e: DataIntegrityViolationException) {
        throw StaleState()
    }

    class NegativeAmount : RuntimeException()
    class InsufficientFunds : RuntimeException()
    class StaleState : RuntimeException()

    companion object {
        private fun requireNotNegative(amount: BigDecimal) {
            if (amount.toDouble() < 0) {
                throw NegativeAmount()
            }
        }
    }
}