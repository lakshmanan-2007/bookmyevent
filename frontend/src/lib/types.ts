export interface AuthUser {
  token: string;
  userId: number;
  name: string;
  role: "USER" | "ADMIN";
}

export interface EventItem {
  id: number;
  title: string;
  venue: string;
  city: string;
  eventTime: string;
  totalSeats: number;
  availableSeats: number;
  price: number;
  soldOut: boolean;
}

export interface Booking {
  id: number;
  eventId: number;
  eventTitle: string;
  quantity: number;
  totalPrice: number;
  status: "CONFIRMED" | "CANCELLED";
  createdAt: string;
}

/** Spring Data Page shape (only the fields we use). */
export interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface ApiError {
  status: number;
  message: string;
  fieldErrors?: Record<string, string>;
}
