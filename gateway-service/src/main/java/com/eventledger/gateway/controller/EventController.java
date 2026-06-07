package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Events", description = "Transaction event ingestion and retrieval")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events")
    @Operation(summary = "Submit a transaction event")
    public ResponseEntity<EventResponse> submitEvent(@Valid @RequestBody EventRequest request) {
        EventService.EventSubmissionResult result = eventService.submitEvent(request);
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.event());
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Retrieve a single event by ID")
    public EventResponse getEvent(@PathVariable("id") String eventId) {
        return eventService.getEvent(eventId);
    }

    @GetMapping("/events")
    @Operation(summary = "List recent events for an account ordered by event timestamp")
    public List<EventResponse> listEvents(@RequestParam("account") String accountId) {
        return eventService.listEventsByAccount(accountId);
    }
}
