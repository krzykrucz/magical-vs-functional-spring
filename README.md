# Magical transfers

### In the past...
...JavaConfig servlet and IoC configuration based on annotations wasn't much of an improvement comparing to the XML-based config.

### Now...
...a **functional way to configure http routes, security config or even beans**, introduced in Spring 5, comes to the rescue!

## On [master](https://github.com/krzykrucz/magical-transfers/tree/master) branch...
... you'll find a showcase of how to configure your Spring Boot app (along with integration tests!!!) in a fully functional, declarative way, with a little help of a new shiny built-in Kotlin DSL helpers. A Kotlin's **coroutines support in Spring** is also leveraged here end-to-end (replaces the use of `reactor`).

## On a branch [spring-fu](https://github.com/krzykrucz/magical-transfers/tree/spring-fu)...
...you'll find even more functional but experimental Spring configuration with `spring-fu`.

## On a branch [old-way](https://github.com/krzykrucz/magical-transfers/tree/old-way)...
...you'll find the same Spring app, but configured with old good JavaConfig.

## Functionality
This server exposes: 
* 2 RESTful http endpoints for creating and crediting a bank account
* a html resource `Hello world` webpage

These are fully covered by end-to-end integration tests, written in `Spock` framework.
