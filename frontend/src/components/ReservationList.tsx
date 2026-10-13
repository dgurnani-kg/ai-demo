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
  const active = reservations.filter((reservation) => reservation.status === "ACTIVE");

  if (active.length === 0) {
    return <p className="empty">Nothing is reserved.</p>;
  }

  return (
    <ul className="loans">
      {active.map((reservation) => (
        <li key={reservation.id}>
          <div>
            <strong>{reservation.bookTitle}</strong>
            <span className="meta">
              {reservation.memberName}
              {reservation.queuePosition != null && ` · #${reservation.queuePosition} in queue`} ·
              reserved {formatDate(reservation.reservedAt)}
            </span>
          </div>
          <button type="button" onClick={() => onCancel(reservation.id)}>
            Cancel
          </button>
        </li>
      ))}
    </ul>
  );
}
