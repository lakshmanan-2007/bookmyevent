"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { EventItem } from "@/lib/types";
import EventCard from "@/components/EventCard";

export default function HomePage() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listEvents(p);
      setEvents(data.content);
      setTotalPages(Math.max(1, data.totalPages));
      setPage(data.number);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load events");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(0);
  }, [load]);

  return (
    <section>
      <div className="hero">
        <span className="hero-eyebrow">● Live events near you</span>
        <h1>
          Book tickets for events <br />
          you&apos;ll <span className="grad">actually remember</span>.
        </h1>
        <p>Concerts, conferences and comedy nights — reserve your seat before it&apos;s gone.</p>
      </div>

      <div className="section-head">
        <h2>Upcoming events</h2>
        {!loading && !error && <span>{events.length} available</span>}
      </div>

      {error && <p className="form-error">{error}</p>}

      {loading ? (
        <div className="grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton-card" />
          ))}
        </div>
      ) : events.length === 0 ? (
        <p className="empty">
          <span className="empty-emoji">🎫</span>
          No events available right now.
        </p>
      ) : (
        <div className="grid">
          {events.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-ghost" disabled={page === 0} onClick={() => load(page - 1)}>
            Previous
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button className="btn btn-ghost" disabled={page + 1 >= totalPages} onClick={() => load(page + 1)}>
            Next
          </button>
        </div>
      )}
    </section>
  );
}
