package org.apache.coyote.http11.controller;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.model.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;
import org.apache.coyote.http11.HttpStatus;

public class RegisterController extends AbstractController {

    @Override
    protected void doPost(HttpRequest request, HttpResponse response) throws Exception {
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

        Path resourcePath = findResourcePath("/index.html");

        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);

        response(response, HttpStatus.OK, contentType, responseBody);
    }

    @Override
    protected void doGet(HttpRequest request, HttpResponse response) throws Exception {
        Path resourcePath = findResourcePath("/register.html");

        String contentType = Files.probeContentType(resourcePath);
        String responseBody = readResponseBody(resourcePath);

        response(response, HttpStatus.OK, contentType, responseBody);
    }
}
