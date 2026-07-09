import type { AuthUser, Booking, EventItem, Page } from "./types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * Thin fetch wrapper that attaches the JWT and normalises the backend's
 * uniform error body into a thrown Error with a readable message.
 */
async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
      if (body?.fieldErrors) {
        const first = Object.values(body.fieldErrors)[0];
        if (typeof first === "string") message = first;
      }
    } catch {
      /* non-JSON error body: keep default message */
    }
    throw new Error(message);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  register(name: string, email: string, password: string) {
    return request<AuthUser>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    });
  },

  login(email: string, password: string) {
    return request<AuthUser>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
  },

  listEvents(page = 0) {
    return request<Page<EventItem>>(`/api/events?page=${page}&size=12`);
  },

  book(eventId: number, quantity: number, token: string) {
    return request<Booking>(
      "/api/bookings",
      { method: "POST", body: JSON.stringify({ eventId, quantity }) },
      token
    );
  },

  myBookings(token: string, page = 0) {
    return request<Page<Booking>>(`/api/bookings?page=${page}&size=10`, {}, token);
  },

  cancelBooking(id: number, token: string) {
    return request<Booking>(`/api/bookings/${id}/cancel`, { method: "POST" }, token);
  },
};
