package com.eventledger.gateway.controller;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.exception.GlobalExceptionHandler;
import com.eventledger.gateway.config.TraceIdFilter;
import com.eventledger.gateway.metrics.EventMetrics;
import com.eventledger.gateway.metrics.MetricsInterceptor;
import com.eventledger.gateway.domain.EventType;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@org.springframework.context.annotation.Import({GlobalExceptionHandler.class, TraceIdFilter.class,
        MetricsInterceptor.class, EventMetrics.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @Test
    void submitEventReturns503WhenAccountServiceUnavailable() throws Exception {
        when(eventService.submitEvent(any())).thenThrow(new AccountServiceUnavailableException("down"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-503",
                                  "accountId": "acct-1",
                                  "type": "CREDIT",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:00:00Z"
                                }
                                """))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getEventWorksWithoutAccountService() throws Exception {
        when(eventService.getEvent("evt-1")).thenReturn(new EventResponse(
                "evt-1", "acct-1", EventType.CREDIT, new BigDecimal("10.00"),
                "USD", Instant.parse("2026-05-15T14:00:00Z"), Map.of(), Instant.now()
        ));

        mockMvc.perform(get("/events/evt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    @Test
    void rejectsUnknownEventTypeWithMeaningfulError() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-bad-type",
                                  "accountId": "acct-1",
                                  "type": "TRANSFER",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("CREDIT, DEBIT")));
    }

    @Test
    void generatesTraceIdWhenMissing() throws Exception {
        when(eventService.listEventsByAccount("acct-1")).thenReturn(List.of());

        mockMvc.perform(get("/events").param("account", "acct-1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));
    }
}
