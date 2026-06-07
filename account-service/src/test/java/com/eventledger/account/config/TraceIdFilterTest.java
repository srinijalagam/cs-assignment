package com.eventledger.account.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TraceIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void propagatesIncomingTraceId() throws Exception {
        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .header("X-Trace-Id", "trace-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-trace",
                                  "type": "CREDIT",
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "eventTimestamp": "2026-05-15T14:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "trace-abc"));
    }
}
