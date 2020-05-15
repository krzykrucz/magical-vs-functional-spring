package com.krzykrucz.magicaltransfers;

import org.springframework.http.codec.HttpMessageWriter
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver

object DefaultHandlerStrategiesResponseContext : ServerResponse.Context {

    private val strategies: HandlerStrategies = HandlerStrategies.withDefaults()

    override fun messageWriters(): MutableList<HttpMessageWriter<*>> = strategies.messageWriters()

    override fun viewResolvers(): MutableList<ViewResolver> = strategies.viewResolvers()
}
