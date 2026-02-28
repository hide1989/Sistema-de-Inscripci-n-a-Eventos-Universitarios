
package com.example;

import java.util.*;

public class EventService {

    private final Map<String, Integer> capacity = new HashMap<>();
    private final Map<String, List<String>> enrollments = new HashMap<>();

    public EventService() {
        capacity.put("EVENTO1", 3);
        enrollments.put("EVENTO1", new ArrayList<>());
    }

    // Imperativo
    public boolean enroll(String eventId, String user) {
        if (capacity.get(eventId) > enrollments.get(eventId).size()) {
            enrollments.get(eventId).add(user);
            return true;
        }
        return false;
    }

    // Declarativo
    public long countInstitutional() {
        return enrollments.get("EVENTO1").stream()
                .filter(u -> u.endsWith("@pascualbravo.edu.co"))
                .count();
    }
}
