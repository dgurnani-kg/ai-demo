import type { Book } from "../api/client";

type Props = {
  books: Book[];
  busyBookId: number | null;
  onBorrow: (bookId: number) => void;
  onReserve: (bookId: number) => void;
};

export default function BookTable({ books, busyBookId, onBorrow, onReserve }: Props) {
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
        {books.map((book) => (
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
              {book.availableCopies === 0 ? (
                <button
                  type="button"
                  disabled={busyBookId === book.id}
                  onClick={() => onReserve(book.id)}
                >
                  {busyBookId === book.id ? "Reserving…" : "Reserve"}
                </button>
              ) : (
                <button
                  type="button"
                  disabled={busyBookId === book.id}
                  onClick={() => onBorrow(book.id)}
                >
                  {busyBookId === book.id ? "Borrowing…" : "Borrow"}
                </button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
