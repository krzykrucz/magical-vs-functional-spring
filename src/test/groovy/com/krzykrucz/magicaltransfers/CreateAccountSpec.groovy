package com.krzykrucz.magicaltransfers

import static groovy.json.JsonOutput.toJson

class CreateAccountSpec extends IntegrationSpec {

    final static SOME_TRACE = 'trace'

    final static MY_ACCOUNT_NUMBER = '100010001000100010001000'

    def "should create account"() {
        when:
        final response = webClientWithAuth
                .post().uri("/create/$MY_ACCOUNT_NUMBER")
                .header('Trace-Id', SOME_TRACE)
                .exchange()

        then:
        response.expectStatus().isOk()
                .expectHeader().valueEquals('Trace-Id', SOME_TRACE)
                .expectBody()
                .json(toJson([
                        number : MY_ACCOUNT_NUMBER,
                        balance: 0
                ]))
    }

}
