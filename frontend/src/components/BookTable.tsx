import type { Book, Reservation, ReservationStatus } from "../api/client";

type Props = {
  books: Book[];
  reservations: Reservation[];
  busyBookId: number | null;
  onBorrow: (bookId: number) => void;
  onReserve: (bookId: number) => void;
};

/** The active member's standing on a title, if they have an open reservation for it. */
function standingOn(reservations: Reservation[], bookId: number): ReservationStatus | null {
  const open = reservations.find(
    (reservation) =>
      reservation.bookId === bookId &&
      (reservation.status === "WAITING" || reservation.status === "READY"),
  );
  return open?.status ?? null;
}

export default function BookTable({
  books,
  reservations,
  busyBookId,
  onBorrow,
  onReserve,
}: Props) {
  if (books.length === 0) {
    return <p className="empty">Nothing on the shelf right now.</p>;
  }

  return (
    <table className="grid">
      <thead>
        <tr>
          <th>Title</th>
          <th>Author</th>
          <th className="num">On shelf</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {books.map((book) => {
          const standing = standingOn(reservations, book.id);
          const busy = busyBookId === book.id;
          // A copy held for this member is borrowable by them even though the shelf
          // count does not show it as free.
          const canBorrow = book.availableCopies > 0 || standing === "READY";

          return (
            <tr key={book.id}>
              <td>
                <strong>{book.title}</strong>
                <span className="isbn">{book.isbn}</span>
              </td>
              <td>{book.author}</td>
              <td className="num">
                <span className={book.availableCopies > 0 ? "pill ok" : "pill none"}>
                  {book.availableCopies} of {book.totalCopies}
                </span>
              </td>
              <td className="actions">
                {standing === "READY" && <span className="pill ready">Ready for Borrow</span>}
                {canBorrow ? (
                  <button type="button" disabled={busy} onClick={() => onBorrow(book.id)}>
                    {busy ? "Borrowing…" : "Borrow"}
                  </button>
                ) : standing === "WAITING" ? (
                  <button type="button" disabled>
                    In queue
                  </button>
                ) : (
                  <button type="button" disabled={busy} onClick={() => onReserve(book.id)}>
                    {busy ? "Reserving…" : "Reserve"}
                  </button>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
