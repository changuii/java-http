package org.apache.coyote.http11;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.exception.UncheckedServletException;
import com.techcourse.model.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);
    private static final SessionManager SESSION_MANAGER = new SessionManager();
    private static final String HTTP_VERSION = "1.1";

    private final Socket connection;

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (final var inputStream = connection.getInputStream();
             final var outputStream = connection.getOutputStream();
             final var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {

            HttpRequest request = new HttpRequest(bufferedReader);
            HttpResponse response = new HttpResponse(outputStream);
            response.setVersion(HTTP_VERSION);

            handle(request, response);

            response.response();

        } catch (IOException | UncheckedServletException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handle(HttpRequest request, HttpResponse response) throws IOException, URISyntaxException {
        Session session = getSession(request);
        if (request.isGet()) {
            if (request.equalsUri("/login") && session != null) {
                responseFound("/index.html", List.of(), response);
                return;
            }

            Path resourcePath = findResourcePath(request.getUri(), response);
            if (resourcePath == null) {
                return;
            }

            responseOk(resourcePath, response);
            return;
        }

        if (request.isPost()) {
            if (request.equalsUri("/login")) {
                login(request, response);
                return;
            }
            if (request.equalsUri("/register")) {
                register(request, response);
                return;
            }
        }
    }

    private Session getSession(HttpRequest request) {
        if(!request.containsSessionId()){
            return null;
        }

        String sessionId = request.getSessionId();
        return SESSION_MANAGER.findSession(sessionId);
    }

    private void register(HttpRequest request, HttpResponse response) throws IOException, URISyntaxException {
        Map<String, String> body = request.getBody();
        String account = body.get("account");
        String email = body.get("email");
        String password = body.get("password");

        if(InMemoryUserRepository.findByAccount(account).isPresent()){
            response.setHttpStatusCode(HttpStatus.CONFLICT);
            return;
        }
        User user = new User(account, password, email);
        InMemoryUserRepository.save(user);

        responseFound("/index.html", List.of(), response);
    }

    private void responseOk(Path resourcePath, HttpResponse response) throws IOException {
        String responseBody = readResponseBody(resourcePath);
        String contentType = Files.probeContentType(resourcePath);

        response.setHttpStatusCode(HttpStatus.OK);
        response.setHeader("Content-Type", contentType);
        response.setBody(responseBody);
    }

    private void login(HttpRequest request, HttpResponse response)
            throws IOException, URISyntaxException {
        Map<String, String> body = request.getBody();
        String account = body.get("account");
        String password = body.get("password");

        Optional<User> optionalUser = InMemoryUserRepository.findByAccount(account);
        if(optionalUser.isEmpty()){
            responseFound("/401.html", List.of(), response);
            return;
        }

        User user = optionalUser.get();
        if(!user.checkPassword(password)){
            responseFound("/401.html", List.of(), response);
            return;
        }

        String sessionName = "JSESSIONID";
        Session newSession = SESSION_MANAGER.createSession();
        SESSION_MANAGER.add(newSession);
        Cookie cookie = new Cookie(sessionName, newSession.getId());
        String cookieHeader = "Set-Cookie: " + cookie.getName() + "=" + cookie.getValue();
        List<String> headers = List.of(cookieHeader);

        responseFound("/index.html", headers, response);
    }

    private void responseFound(String uri, List<String> headers, HttpResponse response)
            throws IOException, URISyntaxException {
        Path resourcePath = findResourcePath(uri, response);
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

    private Path findResourcePath(String uri, HttpResponse response) throws URISyntaxException {
        URL url = findResourceURL(uri);
        if(url == null){
            response.setBody("Hello World!");
            response.setHttpStatusCode(HttpStatus.OK);
            response.setHeader("Content-Type", "text/html;charset=utf-8");
            return null;
        }

        Path resourcePath = Path.of(url.toURI());
        Path normalizedResourcePath = resourcePath.normalize();
        if(!normalizedResourcePath.startsWith("/")){
            response.setHttpStatusCode(HttpStatus.UNAUTHORIZED);
            return null;
        }

        if(!Files.isRegularFile(normalizedResourcePath)){
            response.setBody("Hello World!");
            response.setHttpStatusCode(HttpStatus.OK);
            response.setHeader("Content-Type", "text/html;charset=utf-8");
            return null;
        }


        return normalizedResourcePath;
    }

    private URL findResourceURL(String uri) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("static" + uri);

        if (url == null) {
            return classLoader.getResource("static" + uri + ".html");
        }
        return url;
    }

    private String readResponseBody(Path resourcePath) throws IOException {
        return String.join("\n", Files.readAllLines(resourcePath)) + "\n";
    }
}
