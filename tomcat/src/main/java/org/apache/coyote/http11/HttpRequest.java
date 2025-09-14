package org.apache.coyote.http11;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequest {

    private static final String REQUEST_LINE_DELIMITER = " ";
    private static final int METHOD_INDEX = 0;
    private static final int URI_INDEX = 1;
    private static final int VERSION_INDEX = 2;

    private static final String HEADER_DELIMITER = ": ";

    private static final String COOKIES_DELIMITER = "; ";
    private static final String COOKIE_DELIMITER = "=";
    private static final int COOKIE_NAME_INDEX = 0;
    private static final int COOKIE_VALUE_INDEX = 1;

    private static final String FORM_BODIES_DELIMITER = "&";
    private static final String FORM_BODY_DELIMITER = "=";

    private String version;
    private String method;
    private String uri;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Cookie> cookies = new HashMap<>();
    private String body;

    public HttpRequest(final BufferedReader reader) throws IOException {
        readRequestLine(reader);
        readHeaders(reader);
        parseCookies();
        readBody(reader);
    }

    private void readRequestLine(BufferedReader reader) throws IOException {
        String requestLineText = reader.readLine();
        validateHttpMessage(requestLineText);

        String[] requestLine = requestLineText.split(REQUEST_LINE_DELIMITER);
        version = requestLine[VERSION_INDEX];
        uri = requestLine[URI_INDEX];
        method = requestLine[METHOD_INDEX];
    }

    private void validateHttpMessage(String httpMessage) {
        if(httpMessage == null || httpMessage.isBlank()){
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
    }

    private void readHeaders(BufferedReader reader) throws IOException {
        String headerText = reader.readLine();
        while (!"".equals(headerText)){
            String[] header = headerText.split(HEADER_DELIMITER);
            headers.put(header[0], header[1]);
            headerText = reader.readLine();
        }
    }

    private void parseCookies() {
        if(headers.containsKey("Cookie")){
            Arrays.stream(headers.get("Cookie").split(COOKIES_DELIMITER))
                        .map(cookie -> cookie.split(COOKIE_DELIMITER))
                        .map(cookie -> new Cookie(cookie[COOKIE_NAME_INDEX], cookie[COOKIE_VALUE_INDEX]))
                        .forEach(cookie -> this.cookies.put(cookie.getName(), cookie));
        }
    }

    private void readBody(BufferedReader reader) throws IOException {
        if(headers.containsKey("Content-Length")){
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] body = new char[contentLength];
            reader.read(body);

            this.body = new String(body);
        }
    }

    public boolean containsSessionId(){
        return cookies.containsKey("JSESSIONID");
    }

    public String getSessionId(){
        if(containsSessionId()){
            return cookies.get("JSESSIONID").getValue();
        }
        return "";
    }

    public boolean isGet(){
        return "GET".equals(method);
    }

    public boolean equalsUri(String uri){
        return this.uri.equals(uri);
    }

    public String getUri(){
        return uri;
    }

    public boolean isPost(){
        return "POST".equals(method);
    }

    public Map<String, String> getBody() {
        if(headers.containsKey("Content-Type")
                && "application/x-www-form-urlencoded".equals(headers.get("Content-Type"))){
            return Arrays.stream(body.split(FORM_BODIES_DELIMITER))
                    .map(body -> body.split(FORM_BODY_DELIMITER))
                    .collect(Collectors.toMap(
                            body -> body[0],
                            body -> body[1]
                    ));
        }

        return new HashMap<>();
    }
}
