package com.swordshield.next.activity;
import com.swordshield.next.common.ApiEnvelope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class EventController {
    private final EventRepository repository;
    private final EventApplicationService service;
    private final ResourceAccessService access;
    public EventController(EventRepository repository,EventApplicationService service,ResourceAccessService access) {
        this.repository=repository;this.service=service;this.access=access;
    }
    public record CreateEventRequest(@NotBlank String title) {}
    // Demonstration-only public site: one configured tenant; production must resolve domain to tenant securely.
    @GetMapping("/api/v1/public/events")
    public ApiEnvelope<List<EventRepository.EventRow>> publishedEvents() {
        return ApiEnvelope.ok(repository.published("default"));
    }
    @PostMapping("/api/v1/admin/events")
    @PreAuthorize("hasAuthority('event:create')")
    public ApiEnvelope<EventRepository.EventRow> create(@RequestBody @Valid CreateEventRequest request,
           @AuthenticationPrincipal Jwt jwt) {
        String tenant=jwt.getClaimAsString("tenant_id");
        if (!"default".equals(tenant)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Unknown tenant");
        return ApiEnvelope.ok(service.create(tenant,request.title(),jwt.getSubject()));
    }
    @GetMapping("/api/v1/admin/events/{id}")
    @PreAuthorize("hasAuthority('event:read')")
    public ApiEnvelope<EventRepository.EventRow> detail(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) {
        String tenant=jwt.getClaimAsString("tenant_id");
        if (!"default".equals(tenant)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Unknown tenant");
        access.require(jwt,"event:read","event",id);
        EventRepository.EventRow found=repository.find(id,tenant);
        if (found==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found");
        return ApiEnvelope.ok(found);
    }
    @PostMapping("/api/v1/admin/events/{id}/publish")
    @PreAuthorize("hasAuthority('event:publish')")
    public ApiEnvelope<EventRepository.EventRow> publish(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) {
        String tenant=jwt.getClaimAsString("tenant_id");
        if (!"default".equals(tenant)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Unknown tenant");
        access.require(jwt,"event:publish","event",id);
        return ApiEnvelope.ok(service.publish(tenant,id));
    }
}
