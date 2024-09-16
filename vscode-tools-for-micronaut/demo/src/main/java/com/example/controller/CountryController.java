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

import java.math.BigInteger;
import java.net.URI;
import java.util.List;

import com.example.entity.Country;
import com.example.repository.CountryRepository;

import io.micronaut.context.annotation.Parameter;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import jakarta.validation.Valid;

/**
 *
 * @author krifoste
 */
@Controller("/country")
public class CountryController {

    private final CountryRepository countryRepository;

    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Get
    public List<Country> list(@Valid Pageable pageable) {
        return countryRepository.findAll(pageable).getContent();
    }

    @Get(uri = "/{regionId}")
    public Country findByRegionId(@Body @Valid BigInteger regionId) {
        return countryRepository.findByRegionId(regionId);
    }

    @Post
    public <S extends Country> HttpResponse<S> save(@Body @Valid S entity) {
        S s = countryRepository.save(entity);
        return HttpResponse.created(s).headers(headers -> headers.location(URI.create("/country/" + entity.getCountryId())));
    }

    
    
}
