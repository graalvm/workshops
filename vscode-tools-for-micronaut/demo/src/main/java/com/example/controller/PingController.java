package com.example.controller;

import java.net.URI;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Status;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author krifoste
 */
@Controller("/ping")
public class PingController {

    @Get(produces = "text/plain")
    public String get() {
        // TODO: review the generated method skeleton and provide a meaningful implementation.
        try {
            TimeUnit.SECONDS.sleep(3);
            return "Example Response";
        } catch (InterruptedException ex) {
            return null;
        }
    }

    @Put
    public HttpResponse update(@Body String value) {
        // TODO: review the generated method skeleton and provide a meaningful implementation.
        return HttpResponse.noContent().header(HttpHeaders.LOCATION, URI.create("/ping/").getPath());
    }
    
}
