package com.krzykrucz.magicaltransfers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import spock.lang.Specification

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity

@SpringBootTest
@ContextConfiguration(initializers = BeansInitializer)
class IntegrationSpec extends Specification {

    WebTestClient webClientWithAuth

    WebTestClient webClientNoAuth

    @Autowired
    ApplicationContext context

    @Autowired
    ReactiveMongoTemplate mongoTemplate


    def setup() {
        webClientNoAuth = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build()
                .mutateWith(csrf())
        webClientWithAuth = webClientNoAuth.mutateWith(mockUser())
    }

    def cleanup() {
        StepVerifier.create(
                mongoTemplate.collectionNames
                        .flatMap { mongoTemplate.dropCollection(it) }
        ).verifyComplete()

    }

}
