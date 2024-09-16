package com.example;

import java.util.Optional;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.server.types.files.StreamedFile;

public interface ProfilePicturesApi {

    @Post(uri = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA) 
    HttpResponse upload(CompletedFileUpload fileUpload, String userId, HttpRequest<?> request);

    @Get("/{userId}") 
    Optional<HttpResponse<StreamedFile>> download(String userId);

    @Status(HttpStatus.NO_CONTENT) 
    @Delete("/{userId}") 
    void delete(String userId);
}