package com.krzykrucz.magicaltransfers

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.server.EntityResponse
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.math.BigDecimal
import java.util.UUID


@SpringBootApplication
class MagicalTransfersApplication

fun main(args: Array<String>) {
    runApplication<MagicalTransfersApplication>(*args) {
        addInitializers(BeansInitializer)
    }
}

object BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) = beans.initialize(applicationContext)
}

val beans = beans {
    bean {
        val http = ref<ServerHttpSecurity>()
        http {
            authorizeExchange {
                authorize(anyExchange, authenticated)
            }
            httpBasic { }
        }
    }
    bean {
        User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build()
            .let { MapReactiveUserDetailsService(it) }
    }
    bean {
        val accountRepository = ref<AccountRepository>()
        routes(accountRepository)
    }
}

private fun routes(accountRepository: AccountRepository): RouterFunction<ServerResponse> {
    return coRouter {
        POST("/credit") { request ->
            val (accountNumber, money) = request.awaitBody<CreditAccountRequest>()
            val account = accountRepository.findById(accountNumber)
                .awaitFirstOrNull()
                ?: throw AccountNotFoundException
            val creditedAccount = account.credit(money)
            val savedAccount = accountRepository.save(creditedAccount).awaitFirst()

            ServerResponse.ok()
                .bodyValueAndAwait(savedAccount)
        }
        POST("/create/{accountNumber}") { request ->
            val accountNumber = request.pathVariable("accountNumber")
            val account = Account(accountNumber, BigDecimal.ZERO)
            val savedAccount = accountRepository.save(account).awaitFirst()

            ServerResponse.ok()
                .bodyValueAndAwait(savedAccount)
        }
        filter { request, handler ->
            val trace = request.headers().firstHeader("Trace-Id") ?: "${UUID.randomUUID()}"
            handler(request)
                .let { response ->
                    val responseBuilder = ServerResponse.from(response)
                        .header("Trace-Id", trace)
                    if (response is EntityResponse<*>) responseBuilder.bodyValueAndAwait(response.entity())
                    else responseBuilder.buildAndAwait()
                }
        }
        onError<Throwable> { error, _ ->
            when (error) {
                is AccountNotFoundException -> HttpStatus.NOT_FOUND
                else -> HttpStatus.INTERNAL_SERVER_ERROR
            }
                .let(ServerResponse::status)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValueAndAwait(error.localizedMessage)
        }
        resources("/**", ClassPathResource("/htmls/"))
    }
}

data class Account(
    @Id val number: String,
    val balance: BigDecimal
) {
    fun credit(money: BigDecimal) = copy(balance = balance + money)
}

interface AccountRepository : ReactiveMongoRepository<Account, String>

object AccountNotFoundException : RuntimeException("Account not found")

data class CreditAccountRequest(
    val accountNumber: String,
    val money: BigDecimal
)

