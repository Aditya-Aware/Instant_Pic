package com.sharedmemory.event;

import com.sharedmemory.common.exception.NotFoundException;
import com.sharedmemory.event.dto.CreateEventRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public Event createEvent(UUID creatorUserId, CreateEventRequest request) {
        Event event = Event.builder()
                .name(request.name())
                .description(request.description())
                .createdByUserId(creatorUserId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(EventStatus.ACTIVE)
                .build();
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Event getById(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    }

    @Transactional(readOnly = true)
    public List<Event> getAllByIds(List<UUID> eventIds) {
        return eventRepository.findAllById(eventIds);
    }
}
