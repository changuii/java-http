package org.apache.coyote.http11.controller;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.model.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.coyote.http11.cookie.Cookie;
import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;
import org.apache.coyote.http11.HttpStatus;
import org.apache.coyote.http11.session.Session;
import org.apache.coyote.http11.session.SessionManager;

public class LoginController extends AbstractController {

    private static final SessionManager SESSION_MANAGER = new SessionManager();

    @Override
    protected void doPost(HttpRequest request, HttpResponse response) throws Exception {
        Map<String, String> body = request.getBody();
        String account = body.get("account");
        String password = body.get("password");

        Optional<User> optionalUser = InMemoryUserRepository.findByAccount(account);
        if (optionalUser.isEmpty()) {
            Path resourcePath = findResourcePath("/401.html");

            String contentType = Files.probeContentType(resourcePath);
            String responseBody = readResponseBody(resourcePath);

            response(response, HttpStatus.UNAUTHORIZED, contentType, responseBody);
            return;
        }

        User user = optionalUser.get();
        if (!user.checkPassword(password)) {
            Path resourcePath = findResourcePath("/401.html");

            String contentType = Files.probeContentType(resourcePath);
            String responseBody = readResponseBody(resourcePath);

            response(response, HttpStatus.UNAUTHORIZED, contentType, responseBody);
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

    @Override
    protected void doGet(HttpRequest request, HttpResponse response) throws Exception {
        Session session = request.getSession();
        if (session != null) {
            Path resourcePath = findResourcePath("/index.html");

            String contentType = Files.probeContentType(resourcePath);
            String responseBody = readResponseBody(resourcePath);

            response(response, HttpStatus.FOUND, contentType, responseBody);
            return;
        }

        Path resourcePath = findResourcePath("/login.html");

        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);

        response(response, HttpStatus.OK, contentType, responseBody);
    }
}
