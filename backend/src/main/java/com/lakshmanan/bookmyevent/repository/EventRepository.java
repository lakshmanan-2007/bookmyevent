package com.lakshmanan.bookmyevent.repository;

import com.lakshmanan.bookmyevent.domain.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Loads an event with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE).
     * Any other transaction trying to book the same event blocks until this
     * transaction commits, which is what prevents overselling the last seats.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
