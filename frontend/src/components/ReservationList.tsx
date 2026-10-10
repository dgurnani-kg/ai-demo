import type { Reservation } from "../api/client";

type Props = {
  reservations: Reservation[];
  onCancel: (reservationId: number) => void;
};

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function ReservationList({ reservations, onCancel }: Props) {
  // COLLECTED and CANCELLED are closed; only the queue is worth showing.
  const open = reservations.filter(
    (reservation) => reservation.status === "WAITING" || reservation.status === "READY",
  );

  if (open.length === 0) {
    return <p className="empty">No one is waiting.</p>;
  }

  return (
    <ul className="loans">
      {open.map((reservation) => (
        <li key={reservation.id}>
          <div>
            <strong>{reservation.bookTitle}</strong>
            <span className="meta">
              {reservation.memberName} · reserved {formatDate(reservation.reservedAt)}
            </span>
          </div>
          <div className="row">
            <span className={reservation.status === "READY" ? "pill ready" : "pill waiting"}>
              {reservation.status === "READY" ? "Ready" : "Waiting"}
            </span>
            <button type="button" onClick={() => onCancel(reservation.id)}>
              Cancel
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
