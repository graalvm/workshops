/*
 * Copyright 2024 krifoste.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
