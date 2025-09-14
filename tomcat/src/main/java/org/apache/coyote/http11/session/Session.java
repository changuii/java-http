package org.apache.coyote.http11.session;

import java.util.HashMap;
import java.util.Map;

public class Session {
    private final String id;
    private final Map<String, Object> values = new HashMap<>();

    public Session(String id) {
        this.id = id;
    }

    public void add(String key, Object value){
        values.put(key, value);
    }

    public void remove(String key){
        values.remove(key);
    }

    public Object getValue(String key){
        return values.get(key);
    }

    public String getId(){
        return id;
    }
}
