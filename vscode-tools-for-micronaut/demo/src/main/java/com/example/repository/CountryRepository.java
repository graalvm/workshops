package com.example.repository;

import java.math.BigInteger;

import com.example.entity.Country;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;

/**
 *
 * @author krifoste
 */
@JdbcRepository(dialect = Dialect.ORACLE)
public interface CountryRepository extends PageableRepository<Country, String> {

    Country findByRegionId(BigInteger regionId);
}
