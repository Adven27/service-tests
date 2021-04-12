package com.example.demo

import org.springframework.data.repository.CrudRepository
import java.io.Serializable
import java.math.BigDecimal
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Version

@Entity
data class Account(
    @field:EmbeddedId var accountId: AccountId,
    var balance: BigDecimal,
    @Version private val version: Long = 0
) {
    constructor(user: Long, currency: String, balance: BigDecimal) : this(AccountId(user, currency), balance)

    fun deposit(amount: BigDecimal) = this.apply { balance = balance.add(amount) }

    fun withdraw(amount: BigDecimal) = this.apply { balance = balance.subtract(amount) }

    val user: Long
        get() = accountId.userId
    val currency: String
        get() = accountId.currency
}

@Embeddable
data class AccountId(var userId: Long, var currency: String) : Serializable

interface AccountRepo : CrudRepository<Account, AccountId> {
    fun findByAccountIdUserId(userId: Long): List<Account>
}