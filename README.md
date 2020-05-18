# Magical transfers

JavaConfig servlet and IoC configuration based on annotations wasn't much of an improvement comparing to the XML-based config with regard to how much magic happens underneath.

**A functional way to configure http routes, security config or even beans, introduced in Spring 5**, comes to the rescue!

It is a showcase of how to configure your Spring Boot app (along with integration tests) in a fully functional, declarative way, with a little help of a new shiny built-in Kotlin DSL helpers. A Kotlin's **coroutines support in Spring** is also leveraged here end-to-end (replaces the use of `reactor`).

**On a branch [spring-fu](https://github.com/krzykrucz/magical-transfers/tree/old-way)** you'll find even more functional but experimental Spring configuration with `spring-fu`.

**On a branch [old-way](https://github.com/krzykrucz/magical-transfers/tree/old-way)** you'll find the same Spring app, but configured with old good JavaConfig.

This server exposes 2 RESTful http endpoints for creating and crediting a bank account, and also a html resource `Hello world` webpage
