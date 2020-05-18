package com.krzykrucz.magicaltransfers

import org.springframework.boot.autoconfigure.data.mongo.MongoCoroutineDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoCoroutineRepositoriesAutoConfiguration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.CoroutineMongoTemplate
import org.springframework.data.mongodb.repository.CoroutineMongoRepository
import org.springframework.data.mongodb.repository.support.CoroutineMongoRepositoryFactory
import org.springframework.fu.kofu.application
import org.springframework.fu.kofu.mongo.reactiveMongodb
import org.springframework.fu.kofu.webflux.webFlux
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.EntityResponse
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.math.BigDecimal
import java.util.UUID

val app = application {
    beans {
        bean {
            CoroutineMongoTemplate(ref())
                .let(::CoroutineMongoRepositoryFactory)
                .getRepository(AccountRepository::class.java)
        }
        bean<MongoCoroutineDataAutoConfiguration>()
        bean<MongoCoroutineRepositoriesAutoConfiguration>()
        /* FIXME there will be a separate security config in kofu.application when Spring Security DSL gets released
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
        */
    }
    webFlux {
        coRouter {
            val accountRepository = ref<AccountRepository>()
            POST("/credit") { request ->
                val (accountNumber, money) = request.awaitBody<CreditAccountRequest>()
                val account = accountRepository.findById(accountNumber)
                    ?: throw AccountNotFoundException
                val creditedAccount = account.credit(money)
                val savedAccount = accountRepository.save(creditedAccount)

                ServerResponse.ok().bodyValueAndAwait(savedAccount)
            }
            POST("/create/{accountNumber}") { request ->
                val accountNumber = request.pathVariable("accountNumber")
                Account(accountNumber, BigDecimal.ZERO)
                    .let { accountRepository.save(it) }
                    .let { ServerResponse.ok().bodyValueAndAwait(it) }
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
                val status = when (error) {
                    is AccountNotFoundException -> HttpStatus.NOT_FOUND
                    else -> HttpStatus.INTERNAL_SERVER_ERROR
                }
                ServerResponse.status(status)
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValueAndAwait(error.localizedMessage)
            }
            resources("/**", ClassPathResource("/htmls/"))
        }
        codecs {
            string()
            jackson()
            resource()
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

interface AccountRepository : CoroutineMongoRepository<Account, String>

object AccountNotFoundException : RuntimeException("Account not found")

data class CreditAccountRequest(
    val accountNumber: String,
    val money: BigDecimal
)

