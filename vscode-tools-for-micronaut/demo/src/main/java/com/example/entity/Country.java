package com.example.entity;

import java.math.BigInteger;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

/**
 *
 * @author krifoste
 */
@Serdeable
@MappedEntity("COUNTRIES")
public class Country {

    @Id
    private String countryId;
    private String countryName;
    private BigInteger regionId;

    public Country() {
    }

    public Country(String countryId) {
        this.countryId = countryId;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public BigInteger getRegionId() {
        return regionId;
    }

    public void setRegionId(BigInteger regionId) {
        this.regionId = regionId;
    }

}
