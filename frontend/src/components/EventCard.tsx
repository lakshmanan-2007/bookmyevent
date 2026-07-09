"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { EventItem } from "@/lib/types";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function CalendarIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </svg>
  );
}

function PinIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0Z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  );
}

export default function EventCard({ event }: { event: EventItem }) {
  const { user } = useAuth();
  const router = useRouter();
  const [qty, setQty] = useState(1);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<{ type: "success" | "error"; msg: string } | null>(null);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3200);
    return () => clearTimeout(t);
  }, [toast]);

  async function handleBook() {
    if (!user) {
      router.push("/login");
      return;
    }
    setBusy(true);
    try {
      await api.book(event.id, qty, user.token);
      setToast({ type: "success", msg: `Booked ${qty} ticket(s) for ${event.title}` });
      router.refresh();
    } catch (err) {
      setToast({ type: "error", msg: err instanceof Error ? err.message : "Booking failed" });
    } finally {
      setBusy(false);
    }
  }

  const maxQty = Math.min(10, Math.max(1, event.availableSeats));

  return (
    <article className="card">
      <div className="card-banner">
        <span className="card-price-tag">₹{event.price.toFixed(0)}</span>
      </div>
      <div className="card-body">
        <div className="card-top">
          <h3>{event.title}</h3>
          <span className={`pill ${event.soldOut ? "pill-red" : event.availableSeats <= 5 ? "pill-amber" : "pill-green"}`}>
            {event.soldOut ? "Sold out" : `${event.availableSeats} left`}
          </span>
        </div>

        <div className="meta-row">
          <PinIcon />
          {event.venue}, {event.city}
        </div>
        <div className="meta-row">
          <CalendarIcon />
          {formatDate(event.eventTime)}
        </div>

        <div className="card-actions">
          <select
            className="select"
            value={qty}
            onChange={(e) => setQty(Number(e.target.value))}
            disabled={event.soldOut || busy}
            aria-label="Ticket quantity"
          >
            {Array.from({ length: maxQty }).map((_, i) => (
              <option key={i + 1} value={i + 1}>
                {i + 1}
              </option>
            ))}
          </select>
          <button className="btn btn-primary" onClick={handleBook} disabled={event.soldOut || busy}>
            {busy ? "Booking…" : event.soldOut ? "Sold out" : "Book now"}
          </button>
        </div>
      </div>

      {toast && (
        <div className="toast-wrap">
          <div className={`toast ${toast.type}`}>
            <span className="dot" />
            {toast.msg}
          </div>
        </div>
      )}
    </article>
  );
}
