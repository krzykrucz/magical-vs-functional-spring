package com.krzykrucz.magicaltransfers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.CoroutineMongoRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID


@SpringBootApplication
class MagicalTransfersApplication

fun main(args: Array<String>) {
    runApplication<MagicalTransfersApplication>(*args)
}

data class Account(
    @Id val number: String,
    val balance: BigDecimal
) {
    fun credit(money: BigDecimal) = copy(balance = balance + money)
}

interface AccountRepository : CoroutineMongoRepository<Account, String>

object AccountNotFoundException : RuntimeException("Account not found")

@RestController
class AccountController(val accountRepository: AccountRepository) {

    @PostMapping("/credit")
    suspend fun creditAccount(@RequestBody request: CreditAccountRequest): Account {
        val account = accountRepository.findById(request.accountNumber)
            ?: throw AccountNotFoundException
        val creditedAccount = account.credit(request.money)
        return accountRepository.save(creditedAccount)
    }

    @PostMapping("/create/{accountNumber}")
    suspend fun createAccount(@PathVariable("accountNumber") accountNumber: String): Account =
        Account(accountNumber, BigDecimal.ZERO)
            .let { accountRepository.save(it) }
}

data class CreditAccountRequest(
    val accountNumber: String,
    val money: BigDecimal
)

@Component
class GlobalErrorWebExceptionHandler : ErrorWebExceptionHandler {
    override fun handle(exchange: ServerWebExchange, error: Throwable): Mono<Void> {
        val status = when (error) {
            is AccountNotFoundException -> HttpStatus.NOT_FOUND
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        return ServerResponse.status(status)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(error.localizedMessage)
            .flatMap { it.writeTo(exchange, DefaultResponseContext) }
    }

}

@Component
class TracingWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, webFilterChain: WebFilterChain): Mono<Void> {
        val trace = exchange.request.headers["Trace-Id"]?.get(0) ?: "${UUID.randomUUID()}"
        exchange.response.headers.add("Trace-Id", trace)
        return webFilterChain.filter(exchange)
    }
}

@Configuration
class SecurityConfig {

    @Bean
    fun userDetailsService(): MapReactiveUserDetailsService =
        User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build()
            .let { MapReactiveUserDetailsService(it) }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http.authorizeExchange()
            .anyExchange().authenticated()
            .and()
            .httpBasic()
            .and()
            .build()
}
