package gymmie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrivialTest {
    @Test
    void appTitleIsGymmie() {
        assertEquals("Gymmie", App.getAppTitle());
    }
}
