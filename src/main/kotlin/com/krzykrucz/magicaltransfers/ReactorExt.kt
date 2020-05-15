package com.krzykrucz.magicaltransfers

import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

fun <T> Mono<T>.failIfEmpty(exception: Throwable): Mono<T> = switchIfEmpty { exception.toMono() }
