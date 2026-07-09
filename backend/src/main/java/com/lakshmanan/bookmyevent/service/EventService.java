package com.lakshmanan.bookmyevent.service;

import com.lakshmanan.bookmyevent.domain.Event;
import com.lakshmanan.bookmyevent.dto.event.EventRequest;
import com.lakshmanan.bookmyevent.dto.event.EventResponse;
import com.lakshmanan.bookmyevent.exception.ResourceNotFoundException;
import com.lakshmanan.bookmyevent.repository.EventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Page<EventResponse> list(Pageable pageable) {
        return eventRepository.findAll(pageable).map(EventResponse::from);
    }

    @Cacheable(value = "events", key = "#id")
    public EventResponse getById(Long id) {
        return EventResponse.from(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse create(EventRequest request) {
        Event event = new Event(
                request.title(), request.venue(), request.city(),
                request.eventTime(), request.totalSeats(), request.price());
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse update(Long id, EventRequest request) {
        Event event = findOrThrow(id);
        int alreadyBooked = event.getTotalSeats() - event.getAvailableSeats();
        event.setTitle(request.title());
        event.setVenue(request.venue());
        event.setCity(request.city());
        event.setEventTime(request.eventTime());
        event.setPrice(request.price());
        // Keep availability consistent if capacity was changed.
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(Math.max(0, request.totalSeats() - alreadyBooked));
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void delete(Long id) {
        Event event = findOrThrow(id);
        eventRepository.delete(event);
    }

    private Event findOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }
}
