import type { components } from "./schema";

/**
 * Every wire type comes from the generated schema. Nothing here is hand-written.
 *
 * If you need a field the API does not return, the fix is a contract change
 * (.claude/skills/api-contract-change), not a local interface. `npm run check`
 * regenerates these types and fails the build if the UI reads something the
 * contract never promised.
 */
export type Book = components["schemas"]["Book"];
export type Member = components["schemas"]["Member"];
export type Loan = components["schemas"]["Loan"];
export type Reservation = components["schemas"]["Reservation"];
export type ReservationStatus = components["schemas"]["ReservationStatus"];
export type ApiError = components["schemas"]["ApiError"];

export class ApiFailure extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiFailure";
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });

  if (!response.ok) {
    let message = `Request failed with ${response.status}`;
    try {
      const body = (await response.json()) as ApiError;
      message = body.message;
    } catch {
      // The server did not return the ApiError shape. Keep the generic message.
    }
    throw new ApiFailure(response.status, message);
  }

  return (await response.json()) as T;
}

export const api = {
  listBooks: (onlyAvailable = false) =>
    request<Book[]>(`/api/books${onlyAvailable ? "?available=true" : ""}`),

  listMembers: () => request<Member[]>("/api/members"),

  listLoans: (memberId?: number) =>
    request<Loan[]>(`/api/loans${memberId ? `?memberId=${memberId}` : ""}`),

  borrow: (bookId: number, memberId: number) =>
    request<Loan>("/api/loans", {
      method: "POST",
      body: JSON.stringify({ bookId, memberId }),
    }),

  returnLoan: (loanId: number) =>
    request<Loan>(`/api/loans/${loanId}/return`, { method: "POST" }),

  reserve: (bookId: number, memberId: number) =>
    request<Reservation>(`/api/books/${bookId}/reservations`, {
      method: "POST",
      body: JSON.stringify({ memberId }),
    }),

  listReservations: (memberId?: number, status?: ReservationStatus) => {
    const query = new URLSearchParams();
    if (memberId) query.set("memberId", String(memberId));
    if (status) query.set("status", status);
    const suffix = query.toString();
    return request<Reservation[]>(`/api/reservations${suffix ? `?${suffix}` : ""}`);
  },

  cancelReservation: (reservationId: number) =>
    request<Reservation>(`/api/reservations/${reservationId}/cancel`, { method: "POST" }),
};
