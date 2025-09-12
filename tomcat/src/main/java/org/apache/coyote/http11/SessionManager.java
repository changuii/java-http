package org.apache.coyote.http11;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    public Session createSession(){
        String id = UUID.randomUUID().toString();
        return new Session(id);
    }

    public void add(Session session) {
        SESSIONS.put(session.getId(), session);
    }

    public Session findSession(String id) {
        return SESSIONS.get(id);
    }

    public void remove(String id) {
        SESSIONS.remove(id);
    }
}
