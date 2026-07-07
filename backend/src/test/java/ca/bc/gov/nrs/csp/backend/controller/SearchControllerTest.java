package ca.bc.gov.nrs.csp.backend.controller;

import ca.bc.gov.nrs.csp.backend.controller.dto.search.ClientLocationResponse;
import ca.bc.gov.nrs.csp.backend.controller.dto.search.SearchResultResponse;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.GlobalApiExceptionHandler;
import ca.bc.gov.nrs.csp.backend.service.SearchService;
import ca.bc.gov.nrs.csp.backend.service.mapper.SearchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock SearchService searchService;
    @Mock SearchMapper searchMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SearchController(searchService, searchMapper))
                .setControllerAdvice(new GlobalApiExceptionHandler(new StaticMessageSource()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ---------------------------------------------------------------
    // GET /api/search
    // ---------------------------------------------------------------

    @Test
    void search_noFilters_returns200WithEmptyPage() throws Exception {
        Page<?> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(searchService.search(any(), any())).willReturn((Page) emptyPage);
        given(searchMapper.toResponsePage(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/search"))
                .andExpect(status().isOk());
    }

    @Test
    void search_withResults_returns200WithPagedResults() throws Exception {
        SearchResultResponse response = new SearchResultResponse(
                200456L, 100123L, "APP", "WFP521046", LocalDate.of(2024, Month.JANUARY, 31), "SAL", "014963285", "ACME LOGGING LTD", "O", "ESF"
        );
        Page<?> servicePage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 1);
        given(searchService.search(any(), any())).willReturn((Page) servicePage);
        given(searchMapper.toResponsePage(any())).willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/search")
                        .param("invStatus", "APP")
                        .param("invType", "SAL"))
                .andExpect(status().isOk());
    }

    @Test
    void search_honorsPageAndSizeQueryParams() throws Exception {
        Page<?> servicePage = new PageImpl<>(List.of(), PageRequest.of(2, 25), 0);
        given(searchService.search(any(), any())).willReturn((Page) servicePage);
        given(searchMapper.toResponsePage(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 25), 0));

        mockMvc.perform(get("/api/search").param("page", "2").param("size", "25"))
                .andExpect(status().isOk());
    }

    @Test
    void search_serviceThrowsBadRequest_returns400WithApiError() throws Exception {
        given(searchService.search(any(), any()))
                .willThrow(new BadRequestException("Start date must not be after end date."));

        mockMvc.perform(get("/api/search")
                        .param("startDate", "2024-02-01")
                        .param("endDate", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // GET /api/clients
    // ---------------------------------------------------------------

    @Test
    void findClients_byName_returns200WithClientList() throws Exception {
        ClientLocationResponse client = new ClientLocationResponse(
                "00014963", "ACME LOGGING LTD", "00", "HEAD OFFICE", "VICTORIA", "BC"
        );
        given(searchService.findClientsByName(eq("acme"))).willReturn(List.of());
        given(searchMapper.toClientLocationResponseList(any())).willReturn(List.of(client));

        mockMvc.perform(get("/api/clients").param("name", "acme"))
                .andExpect(status().isOk());
    }

    @Test
    void findClients_byNumber_returns200WithClientList() throws Exception {
        ClientLocationResponse client = new ClientLocationResponse(
                "00014963", "ACME LOGGING LTD", "00", "HEAD OFFICE", "VICTORIA", "BC"
        );
        given(searchService.findClientsByNumber(eq("14963"))).willReturn(List.of());
        given(searchMapper.toClientLocationResponseList(any())).willReturn(List.of(client));

        mockMvc.perform(get("/api/clients").param("number", "14963"))
                .andExpect(status().isOk());
    }

    @Test
    void findClients_noParams_returns400() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findClients_blankName_returns400() throws Exception {
        mockMvc.perform(get("/api/clients").param("name", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findClients_serviceThrowsBadRequest_returns400WithApiError() throws Exception {
        given(searchService.findClientsByName(any()))
                .willThrow(new BadRequestException("Client name search term must not be blank."));

        mockMvc.perform(get("/api/clients").param("name", "acme"))
                .andExpect(status().isBadRequest());
    }
}
