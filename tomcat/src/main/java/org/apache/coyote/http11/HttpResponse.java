package org.apache.coyote.http11;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HttpResponse {

    private final OutputStream outputStream;

    private String version;
    private HttpStatus httpStatus;
    private List<String> headers = new ArrayList<>();
    private String body;

    public HttpResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setHttpStatusCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setHeader(String key, String value) {
        this.headers.add(key + ": " + value);
    }

    public void setHeaders(List<String> headers) {
        for (String header : headers) {
            this.headers.add(header);
        }
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void response() throws IOException {
        if (httpStatus == null) {
            throw new IllegalArgumentException("잘못된 응답입니다.");
        }
        String responseLine = String.format("HTTP/%s %d %s", version, httpStatus.getStatusCode(),
                httpStatus.name());

        if (body != null) {
            setHeader("Content-Length", String.valueOf(body.getBytes().length));
        }
        String headers = this.headers.stream().collect(Collectors.joining("\r\n"));

        if (body == null) {
            body = "";
        }

        String httpMessage = String.join("\r\n",
                responseLine,
                headers,
                "",
                body
        );

        outputStream.write(httpMessage.getBytes());
        outputStream.flush();
    }
}
