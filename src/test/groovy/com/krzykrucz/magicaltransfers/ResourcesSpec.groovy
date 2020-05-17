package com.krzykrucz.magicaltransfers

class ResourcesSpec extends IntegrationSpec {

    def "should provide resource"() {
        expect:
        webClientWithAuth
                .get().uri('/index.html')
                .exchange()
                .expectStatus().isOk()
    }

}
