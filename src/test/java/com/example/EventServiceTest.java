
package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EventServiceTest {

    @Test
    void shouldEnrollUserWhenCapacityAvailable() {
        EventService service = new EventService();
        assertTrue(service.enroll("EVENTO1", "ana@pascualbravo.edu.co"));
    }
}
