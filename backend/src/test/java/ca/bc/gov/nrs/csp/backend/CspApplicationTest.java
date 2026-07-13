package ca.bc.gov.nrs.csp.backend;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CspApplicationTest {

    @Test
    void main_delegatesToSpringApplicationRun() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            CspApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(CspApplication.class, args));
        }
    }

    @Test
    void constructor_instantiates() {
        assertNotNull(new CspApplication());
    }
}
