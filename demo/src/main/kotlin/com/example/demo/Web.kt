package com.example.demo

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.math.BigDecimal

@Controller
class WalletController(val walletService: WalletService) {

    @PostMapping("/deposit")
    @ResponseStatus(OK)
    @ResponseBody
    fun deposit(@RequestBody req: WalletRequest) =
        walletService.deposit(req.user, req.amount.toBigDecimal(), req.currency).let {
            WalletResponse(it.user, it.balance, it.currency)
        }

    @PostMapping("/withdraw")
    @ResponseStatus(OK)
    @ResponseBody
    fun withdraw(@RequestBody req: WalletRequest) =
        walletService.withdraw(req.user, req.amount.toBigDecimal(), req.currency).let {
            WalletResponse(it.user, it.balance, it.currency)
        }

    @GetMapping("/balance/{user}")
    @ResponseBody
    fun balance(@PathVariable user: Long) = BalanceResponse(walletService.balance(user))
}

@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(value = [(WalletService.NegativeAmount::class)])
    fun handleNegativeAmount(ex: WalletService.NegativeAmount): ResponseEntity<Error> =
        ResponseEntity(Error("Negative amount", ex.message ?: ""), BAD_REQUEST)

    @ExceptionHandler(value = [(WalletService.InsufficientFunds::class)])
    fun handleInsufficientFunds(ex: WalletService.InsufficientFunds): ResponseEntity<Error> =
        ResponseEntity(Error("Insufficient funds", ex.message ?: ""), BAD_REQUEST)

    @ExceptionHandler(value = [(HttpMessageNotReadableException::class)])
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<Error> =
        ResponseEntity(Error("Message not readable", ex.message ?: ""), BAD_REQUEST)

    data class Error(val message: String, val details: String)
}

data class WalletRequest(val user: Long, val amount: String, val currency: String)
data class WalletResponse(val user: Long, val balance: BigDecimal, val currency: String)
data class BalanceResponse(val balances: Map<String, BigDecimal>)