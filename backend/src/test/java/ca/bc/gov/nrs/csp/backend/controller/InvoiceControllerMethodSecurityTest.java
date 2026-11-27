package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.invoiceDetails.ChangeStatusRequest;
import ca.bc.gov.nrs.csp.backend.service.InvoiceService;
import ca.bc.gov.nrs.csp.backend.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The standalone MockMvc tests skip method security, so this checks the changeStatus
// @PreAuthorize passes the requested status to the permission check.
@SpringJUnitConfig
class InvoiceControllerMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {
        @Bean
        InvoiceService invoiceService() {
            return mock(InvoiceService.class);
        }

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        InvoiceController invoiceController(InvoiceService invoiceService) {
            return new InvoiceController(invoiceService);
        }
    }

    @Autowired
    private InvoiceController controller;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void resetSharedMocks() {
        reset(invoiceService, permissionService);
    }

    // Body validation runs while Spring MVC resolves arguments, before the security proxy, so this is a 400.
    @Test
    @WithMockUser
    void changeStatus_unknownStatus_isA400BeforeTheAuthorizationCheck() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(patch("/api/invoices/1/status?expectedVersion=v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"XYZ\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(permissionService, invoiceService);
    }

    @Test
    @WithMockUser
    void changeStatus_isAuthorizedForTheRequestedStatus() {
        given(permissionService.canChangeInvoiceStatusTo(any(), eq("APP"))).willReturn(true);
        given(permissionService.canChangeInvoiceStatusTo(any(), eq("UNA"))).willReturn(false);

        controller.changeStatus(1L, "v1", new ChangeStatusRequest("APP", null));
        verify(invoiceService).changeStatus(eq(1L), eq("v1"), any());

        ChangeStatusRequest unapprove = new ChangeStatusRequest("UNA", "reopen");
        assertThatThrownBy(() -> controller.changeStatus(1L, "v1", unapprove))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoMoreInteractions(invoiceService);
    }
}
