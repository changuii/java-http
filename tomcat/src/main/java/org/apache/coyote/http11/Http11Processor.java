package org.apache.coyote.http11;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.exception.UncheckedServletException;
import com.techcourse.model.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);
    private static final SessionManager SESSION_MANAGER = new SessionManager();

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
            Session session = getSession(request);

            if(request.isGet()){
                if(request.equalsUri("/login") && session != null){
                    responseFound("/index.html", List.of(), outputStream);
                    return;
                }

                Path resourcePath = findResourcePath(request.getUri(), outputStream);
                if (resourcePath == null) {
                    return;
                }

                responseOk(resourcePath, outputStream);
                return;
            }

            if(request.isPost()){
                if(request.equalsUri("/login")){
                    login(request, outputStream);
                    return;
                }
                if(request.equalsUri("/register")){
                    register(request, outputStream);
                    return;
                }
            }


        } catch (IOException | UncheckedServletException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static Session getSession(HttpRequest request) {
        if(!request.containsSessionId()){
            return null;
        }

        String sessionId = request.getSessionId();
        return SESSION_MANAGER.findSession(sessionId);
    }

    private void register(HttpRequest request, OutputStream outputStream) throws IOException, URISyntaxException {
        Map<String, String> body = request.getBody();
        String account = body.get("account");
        String email = body.get("email");
        String password = body.get("password");

        if(InMemoryUserRepository.findByAccount(account).isPresent()){
            outputStream.write("HTTP/1.1 409 CONFLICT \r\n\r\n".getBytes());
            outputStream.flush();
            return;
        }
        User user = new User(account, password, email);
        InMemoryUserRepository.save(user);

        responseFound("/index.html", List.of(), outputStream);
    }

    private void responseOk(Path resourcePath, OutputStream outputStream) throws IOException {
        String responseBody = readResponseBody(resourcePath);
        String contentType = Files.probeContentType(resourcePath);
        String httpResponse = createHttpResponse(200, "OK" ,List.of(), contentType, responseBody);
        outputStream.write(httpResponse.getBytes());
        outputStream.flush();
    }

    private void login(HttpRequest request, OutputStream outputStream)
            throws IOException, URISyntaxException {
        Map<String, String> body = request.getBody();
        String account = body.get("account");
        String password = body.get("password");

        Optional<User> optionalUser = InMemoryUserRepository.findByAccount(account);
        if(optionalUser.isEmpty()){
            responseFound("/401.html", List.of(), outputStream);
            return;
        }

        User user = optionalUser.get();
        if(!user.checkPassword(password)){
            responseFound("/401.html", List.of(), outputStream);
            return;
        }

        String sessionName = "JSESSIONID";
        Session newSession = SESSION_MANAGER.createSession();
        SESSION_MANAGER.add(newSession);
        Cookie cookie = new Cookie(sessionName, newSession.getId());
        String cookieHeader = "Set-Cookie: " + cookie.getName() + "=" + cookie.getValue();
        List<String> headers = List.of(cookieHeader);

        responseFound("/index.html", headers, outputStream);
    }

    private void responseFound(String uri, List<String> headers, OutputStream outputStream) throws IOException, URISyntaxException {
        Path resourcePath = findResourcePath(uri, outputStream);
        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);
        String httpResponse = createHttpResponse(302, "FOUND", headers, contentType, responseBody);
        outputStream.write(httpResponse.getBytes());
        outputStream.flush();
    }

    private Path findResourcePath(String uri, OutputStream outputStream) throws IOException, URISyntaxException {
        URL url = findResourceURL(uri);
        if(url == null){
            String helloWorldHttpResponse = helloWorldHttpResponse();
            outputStream.write(helloWorldHttpResponse.getBytes());
            outputStream.flush();
            return null;
        }

        Path resourcePath = Path.of(url.toURI());
        Path normalizedResourcePath = resourcePath.normalize();
        if(!normalizedResourcePath.startsWith("/")){
            outputStream.write("HTTP/1.1 401 UNAUTHORIZED \r\n\r\n".getBytes());
            outputStream.flush();
            return null;
        }

        if(!Files.isRegularFile(normalizedResourcePath)){
            String helloWorldHttpResponse = helloWorldHttpResponse();
            outputStream.write(helloWorldHttpResponse.getBytes());
            outputStream.flush();
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

    private String helloWorldHttpResponse() {
        return String.join(
                "\r\n",
                "HTTP/1.1 200 OK ",
                "Content-Type: text/html;charset=utf-8 ",
                "Content-Length: 12 ",
                "",
                "Hello world!"
        );
    }

    private String readResponseBody(Path resourcePath) throws IOException {
        return String.join("\n", Files.readAllLines(resourcePath)) + "\n";
    }

    private String createHttpResponse(int statusCode, String statusMessage, List<String> headers, String contentType, String responseBody) {
        if(headers.isEmpty()){
            return String.join(
                    "\r\n",
                    "HTTP/1.1 "+ statusCode + " " + statusMessage,
                    "Content-Type: "+ contentType +";charset=utf-8 ",
                    "Content-Length: " + responseBody.getBytes().length + " ",
                    "",
                    responseBody
            );
        }

        String headerTexts = headers.stream().collect(Collectors.joining("\r\n"));
        return String.join(
                "\r\n",
                "HTTP/1.1 "+ statusCode + " " + statusMessage,
                "Content-Type: "+ contentType +";charset=utf-8 ",
                "Content-Length: " + responseBody.getBytes().length + " ",
                headerTexts,
                "",
                responseBody
        );
    }
}
