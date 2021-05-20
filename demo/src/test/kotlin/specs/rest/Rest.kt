package specs.rest

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import specs.Specs

class RestDeposit : Specs() {
    fun resetSesInteractions() = ENV.ses().resetRequests()
    fun sesInteractionsCount() = interactions().count()
    fun sesNoInteractions() = interactions().count() == 0
    fun getSesResponse(): String = interactions()[0]

    private fun interactions() = ENV.ses().find(postRequestedFor(urlPathEqualTo("/hook"))).map { it.bodyAsString }
}

class RestWithdraw : Specs()
class RestBalance : Specs()
