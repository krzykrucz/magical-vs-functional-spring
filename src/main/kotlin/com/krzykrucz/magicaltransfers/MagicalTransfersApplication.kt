package com.krzykrucz.magicaltransfers

import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.fu.kofu.application
import org.springframework.fu.kofu.mongo.reactiveMongodb
import org.springframework.fu.kofu.webflux.webFlux
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.server.EntityResponse
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToMono
import java.math.BigDecimal
import java.util.UUID

val app = application {
    beans {
        bean<AccountRepository>()
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
//    bean(::routes)
    }
    webFlux {
        router {
            val accountRepository = ref<AccountRepository>()
            POST("/credit") { request ->
                request.bodyToMono<CreditAccountRequest>()
                    .flatMap { (accountNumber, money) ->
                        accountRepository.findById(accountNumber)
                            .failIfEmpty(AccountNotFoundException)
                            .map { account -> account.credit(money) }
                            .flatMap { accountRepository.save(it) }
                    }
                    .flatMap { ServerResponse.ok().bodyValue(it) }
            }
            POST("/create/{accountNumber}") { request ->
                val accountNumber = request.pathVariable("accountNumber")
                Account(accountNumber, BigDecimal.ZERO)
                    .let(accountRepository::save)
                    .flatMap { ServerResponse.ok().bodyValue(it) }
            }
            filter { request, handler ->
                val trace = request.headers().firstHeader("Trace-Id") ?: "${UUID.randomUUID()}"
                handler(request)
                    .flatMap { response ->
                        val responseBuilder = ServerResponse.from(response)
                            .header("Trace-Id", trace)
                        if (response is EntityResponse<*>) responseBuilder.body(response.inserter())
                        else responseBuilder.build()
                    }
            }
            onError<Throwable> { error, _ ->
                when (error) {
                    is AccountNotFoundException -> HttpStatus.NOT_FOUND
                    else -> HttpStatus.INTERNAL_SERVER_ERROR
                }
                    .let(ServerResponse::status)
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(error.localizedMessage)
            }
            resources("/**", ClassPathResource("/htmls/"))
        }
        codecs {
            string()
            jackson()
        }
    }
    reactiveMongodb {
        embedded()
    }
}

fun main(args: Array<String>) {
    app.run(args)
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

