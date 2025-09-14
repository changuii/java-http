package org.apache.coyote.http11.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;
import org.apache.coyote.http11.HttpStatus;

public class DefaultController extends AbstractController {

    @Override
    protected void doPost(HttpRequest request, HttpResponse response) throws Exception {
        Path resourcePath = findResourcePath("/404.html");

        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);

        response(response, HttpStatus.NOT_FOUND, contentType, responseBody);
    }

    @Override
    protected void doGet(HttpRequest request, HttpResponse response) throws Exception {
        Path resourcePath = findResourcePath(request.getUri());

        if (resourcePath == null) {
            resourcePath = findResourcePath("/404.html");
            String contentType = Files.probeContentType(resourcePath);
            String responseBody = readResponseBody(resourcePath);

            response.setHttpStatusCode(HttpStatus.NOT_FOUND);
            response.setHeader("Content-Type", contentType);
            response.setBody(responseBody);
            return;
        }

        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);

        response(response, HttpStatus.OK, contentType, responseBody);
    }
}
