package com.swordshield.next.activity;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class EventApplicationService {
    private final EventRepository repository;
    public EventApplicationService(EventRepository repository) { this.repository=repository; }
    @Transactional
    public EventRepository.EventRow create(String tenantId,String title,String actorId) {
        String id=UUID.randomUUID().toString();
        repository.create(id,tenantId,title,actorId);
        return repository.find(id,tenantId);
    }
    @Transactional
    public EventRepository.EventRow publish(String tenantId,String eventId) {
        if (repository.publish(eventId,tenantId)!=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Event not found, not owned or not draft");
        return repository.find(eventId,tenantId);
    }
}
