"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { Booking } from "@/lib/types";

export default function BookingsPage() {
  const { user, ready } = useAuth();
  const router = useRouter();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.myBookings(user.token);
      setBookings(data.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load bookings");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (ready && !user) {
      router.replace("/login");
      return;
    }
    if (user) load();
  }, [ready, user, load, router]);

  async function handleCancel(id: number) {
    if (!user) return;
    try {
      await api.cancelBooking(id, user.token);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Cancel failed");
    }
  }

  if (!ready || (!user && loading)) return <p className="loading">Loading…</p>;

  return (
    <section>
      <h1 className="page-title">My bookings</h1>
      <p className="page-sub">Your confirmed and cancelled tickets.</p>

      {error && <p className="form-error">{error}</p>}
      {loading && <p className="loading">Loading…</p>}

      {!loading && bookings.length === 0 && (
        <p className="empty">You haven’t booked anything yet.</p>
      )}

      {bookings.map((b) => (
        <div key={b.id} className="booking-row">
          <div>
            <h4>{b.eventTitle}</h4>
            <p className="muted">
              {b.quantity} ticket(s) · ₹{b.totalPrice.toFixed(2)} ·{" "}
              <span className={b.status === "CONFIRMED" ? "pill pill-green" : "pill pill-red"}>
                {b.status}
              </span>
            </p>
          </div>
          {b.status === "CONFIRMED" && (
            <button className="btn btn-ghost" onClick={() => handleCancel(b.id)}>
              Cancel
            </button>
          )}
        </div>
      ))}
    </section>
  );
}
