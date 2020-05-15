package com.krzykrucz.magicaltransfers

class ResourcesSpec extends IntegrationSpec {

    def "should credit account"() {
        expect:
        webClientWithAuth
                .get().uri('/index.html')
                .exchange()
                .expectStatus().isOk()
    }

}
