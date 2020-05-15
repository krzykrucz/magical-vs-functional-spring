package com.krzykrucz.magicaltransfers

import static groovy.json.JsonOutput.toJson
import static org.springframework.http.MediaType.APPLICATION_JSON

class CreditAccountSpec extends IntegrationSpec {

    final static SOME_TRACE = 'trace'

    final static MY_ACCOUNT_NUMBER = '100010001000100010001000'

    def "should credit account"() {
        given:
        'create account' MY_ACCOUNT_NUMBER

        when:
        final response = webClientWithAuth
                .post().uri('/credit')
                .contentType(APPLICATION_JSON)
                .header('Trace-Id', SOME_TRACE)
                .bodyValue([
                        accountNumber: MY_ACCOUNT_NUMBER,
                        money        : 10.55
                ])
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectHeader().valueEquals('Trace-Id', SOME_TRACE)
                .expectBody()
                .json(toJson([
                        number : MY_ACCOUNT_NUMBER,
                        balance: 10.55
                ]))
    }

    def "should fail on invalid authentication"() {
        when:
        final response = webClientNoAuth
                .post().uri('/credit')
                .contentType(APPLICATION_JSON)
                .header('Trace-Id', SOME_TRACE)
                .bodyValue([
                        accountNumber: MY_ACCOUNT_NUMBER,
                        money        : 10.55
                ])
                .exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    def "should not credit account when no such account"() {
        when:
        final response = webClientWithAuth
                .post().uri('/credit')
                .contentType(APPLICATION_JSON)
                .bodyValue([
                        accountNumber: MY_ACCOUNT_NUMBER,
                        money        : 10.55
                ])
                .exchange()

        then:
        response.expectStatus().isNotFound()
                .expectBody(String).isEqualTo('Account not found')
    }

    def "create account"(number) {
        webClientWithAuth
                .post().uri("/create/$number")
                .exchange()
    }

    def authHeader() {
        new String(Base64.getEncoder().encode("user:password".bytes))
    }

}
