package com.krzykrucz.magicaltransfers


import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity

class IntegrationSpec extends Specification {

    @Shared
    WebTestClient webClientWithAuth

    @Shared
    WebTestClient webClientNoAuth

    @Shared
    ReactiveMongoTemplate mongoTemplate

    @Shared
    ConfigurableApplicationContext context

    def setupSpec() {
        context = MagicalTransfersApplicationKt.app.run([] as String[], 'test', true)
        webClientNoAuth = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(csrf())
        webClientWithAuth = webClientNoAuth.mutateWith(mockUser())
        mongoTemplate = context.getBean(ReactiveMongoTemplate)
    }

    def cleanup() {
        StepVerifier.create(
                mongoTemplate.collectionNames
                        .flatMap { mongoTemplate.dropCollection(it) }
        ).verifyComplete()

    }

    def cleanupSpec() {
        context.close()
    }

    protected def "create account"(number) {
        webClientWithAuth
                .post().uri("/create/$number")
                .exchange()
    }

}
