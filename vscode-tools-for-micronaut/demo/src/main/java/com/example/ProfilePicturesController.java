package com.example;

import java.net.URI;
import java.util.Optional;

import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.http.server.util.HttpHostResolver;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.objectstorage.oraclecloud.OracleCloudStorageEntry;
import io.micronaut.objectstorage.oraclecloud.OracleCloudStorageOperations;
import io.micronaut.objectstorage.request.UploadRequest;
import io.micronaut.objectstorage.response.UploadResponse;
import io.micronaut.scheduling.annotation.ExecuteOn;

@Controller(ProfilePicturesController.PREFIX) 
@ExecuteOn(TaskExecutors.BLOCKING) 
public class ProfilePicturesController implements ProfilePicturesApi {

    static final String PREFIX = "/pictures";

    private final OracleCloudStorageOperations objectStorage; 
    private final HttpHostResolver httpHostResolver; 

    public ProfilePicturesController(OracleCloudStorageOperations objectStorage, HttpHostResolver httpHostResolver) {
        this.objectStorage = objectStorage;
        this.httpHostResolver = httpHostResolver;
    }

    @Override
    public HttpResponse<?> upload(CompletedFileUpload fileUpload, String userId, HttpRequest<?> request) {
        String key = buildKey(userId); 
        UploadRequest objectStorageUpload = UploadRequest.fromCompletedFileUpload(fileUpload, key); 
        UploadResponse<PutObjectResponse> response = objectStorage.upload(objectStorageUpload);  

        return HttpResponse
                .created(location(request, userId)) 
                .header(HttpHeaders.ETAG, response.getETag()); 
    }

    private static String buildKey(String userId) {
        return userId + ".jpg";
    }

    private URI location(HttpRequest<?> request, String userId) {
        return UriBuilder.of(httpHostResolver.resolve(request))
                .path(PREFIX)
                .path(userId)
                .build();
    }

    @Override
    public Optional<HttpResponse<StreamedFile>> download(String userId) {
        String key = buildKey(userId);
        return objectStorage.retrieve(key) 
                .map(ProfilePicturesController::buildStreamedFile); 
    }

    private static HttpResponse<StreamedFile> buildStreamedFile(OracleCloudStorageEntry entry) {
        GetObjectResponse nativeEntry = entry.getNativeEntry();
        MediaType mediaType = MediaType.of(nativeEntry.getContentType());
        StreamedFile file = new StreamedFile(entry.getInputStream(), mediaType).attach(entry.getKey());
        MutableHttpResponse<Object> httpResponse = HttpResponse.ok()
                .header(HttpHeaders.ETAG, nativeEntry.getETag()); 
        file.process(httpResponse);
        return httpResponse.body(file);
    }

    @Override
    public void delete(String userId) {
        String key = buildKey(userId);
        objectStorage.delete(key);
    }
}