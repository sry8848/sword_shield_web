package com.swordshield.next.activity;
import java.util.List;
import org.springframework.stereotype.Repository;
@Repository
public class EventRepository {
    private final EventMapper mapper;
    public EventRepository(EventMapper mapper) { this.mapper=mapper; }
    public record EventRow(String id,String tenantId,String title,String status) {}
    private static EventRow toRow(EventEntity entity) {
        return entity==null ? null : new EventRow(entity.getId(),entity.getTenantId(),entity.getTitle(),entity.getStatus());
    }
    public List<EventRow> published(String tenantId) {
        return mapper.published(tenantId).stream().map(EventRepository::toRow).toList();
    }
    public void create(String id,String tenantId,String title,String actorId) {
        EventEntity event=new EventEntity();
        event.setId(id); event.setTenantId(tenantId); event.setTitle(title);
        event.setStatus("DRAFT"); event.setCreatedBy(actorId);
        if (mapper.insert(event)!=1) throw new IllegalStateException("Failed to create event");
    }
    public int publish(String eventId,String tenantId) { return mapper.publishDraft(eventId,tenantId); }
    public EventRow find(String eventId,String tenantId) { return toRow(mapper.findOne(eventId,tenantId)); }
}
