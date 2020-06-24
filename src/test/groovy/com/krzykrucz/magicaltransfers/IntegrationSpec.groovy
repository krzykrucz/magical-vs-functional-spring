package com.krzykrucz.magicaltransfers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.security.core.userdetails.User
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import spock.lang.Specification

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity

@SpringBootTest
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
        webClientWithAuth = webClientNoAuth.mutateWith(mockUser(
                user('user', 'password', 'USER')))
    }

    def cleanup() {
        StepVerifier.create(
                mongoTemplate.collectionNames
                        .flatMap { mongoTemplate.dropCollection(it) }
        ).verifyComplete()

    }

    protected def "create account"(number) {
        webClientWithAuth
                .post().uri("/create/$number")
                .exchange()
    }

    private static def user(username, password, role) {
        User.withUsername(username)
                .password(password)
                .roles(role)
                .build()
    }

}
