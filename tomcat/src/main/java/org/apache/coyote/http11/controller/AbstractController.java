package org.apache.coyote.http11.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;
import org.apache.coyote.http11.HttpStatus;

public abstract class AbstractController implements Controller {

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        if (request.isGet()) {
            doGet(request, response);
            return;
        }

        if (request.isPost()) {
            doPost(request, response);
            return;
        }
    }

    protected void doPost(HttpRequest request, HttpResponse response) throws Exception {
    }

    protected void doGet(HttpRequest request, HttpResponse response) throws Exception {
    }

    protected void responseFound(String uri, List<String> headers, HttpResponse response)
            throws IOException, URISyntaxException {
        Path resourcePath = findResourcePath(uri);
        if (resourcePath == null) {
            return;
        }

        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);

        response.setHttpStatusCode(HttpStatus.FOUND);
        response.setHeader("Content-Type", contentType);
        response.setHeaders(headers);
        response.setBody(responseBody);
    }

    protected Path findResourcePath(String uri) throws URISyntaxException {
        URL url = findResourceURL(uri);
        if (url == null) {
            return null;
        }

        Path resourcePath = Path.of(url.toURI());
        Path normalizedResourcePath = resourcePath.normalize();
        if (!normalizedResourcePath.startsWith("/")) {
            return null;
        }

        if (!Files.isRegularFile(normalizedResourcePath)) {
            return null;
        }

        return normalizedResourcePath;
    }

    protected URL findResourceURL(String uri) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("static" + uri);

        if (url == null) {
            return classLoader.getResource("static" + uri + ".html");
        }
        return url;
    }

    protected String readResponseBody(Path resourcePath) throws IOException {
        return String.join("\n", Files.readAllLines(resourcePath)) + "\n";
    }

    protected void response(HttpResponse response, HttpStatus httpStatus, String contentType, String responseBody) {
        response.setHttpStatusCode(httpStatus);
        response.setHeader("Content-Type", contentType);
        response.setBody(responseBody);
    }
}
