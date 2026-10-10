import { useCallback, useEffect, useState } from "react";
import {
  api,
  ApiFailure,
  type Book,
  type Loan,
  type Member,
  type Reservation,
} from "../api/client";
import BookTable from "../components/BookTable";
import LoanList from "../components/LoanList";
import ReservationList from "../components/ReservationList";

export default function CataloguePage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [activeMemberId, setActiveMemberId] = useState<number | null>(null);
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [busyBookId, setBusyBookId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const [nextBooks, nextLoans, nextReservations] = await Promise.all([
        api.listBooks(onlyAvailable),
        api.listLoans(),
        api.listReservations(),
      ]);
      setBooks(nextBooks);
      setLoans(nextLoans);
      setReservations(nextReservations);
      setError(null);
    } catch (cause) {
      setError(cause instanceof ApiFailure ? cause.message : "Could not reach the server.");
    }
  }, [onlyAvailable]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const nextMembers = await api.listMembers();
        if (cancelled) return;
        setMembers(nextMembers);
        setActiveMemberId(nextMembers[0]?.id ?? null);
        await refresh();
      } catch (cause) {
        if (!cancelled) {
          setError(cause instanceof ApiFailure ? cause.message : "Could not reach the server.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [refresh]);

  async function borrow(bookId: number) {
    if (activeMemberId === null) return;
    setBusyBookId(bookId);
    try {
      await api.borrow(bookId, activeMemberId);
      await refresh();
    } catch (cause) {
      setError(cause instanceof ApiFailure ? cause.message : "Could not borrow that book.");
    } finally {
      setBusyBookId(null);
    }
  }

  async function reserve(bookId: number) {
    if (activeMemberId === null) return;
    setBusyBookId(bookId);
    try {
      await api.reserve(bookId, activeMemberId);
      await refresh();
    } catch (cause) {
      setError(cause instanceof ApiFailure ? cause.message : "Could not reserve that book.");
    } finally {
      setBusyBookId(null);
    }
  }

  async function returnLoan(loanId: number) {
    try {
      await api.returnLoan(loanId);
      await refresh();
    } catch (cause) {
      setError(cause instanceof ApiFailure ? cause.message : "Could not return that loan.");
    }
  }

  async function cancelReservation(reservationId: number) {
    try {
      await api.cancelReservation(reservationId);
      await refresh();
    } catch (cause) {
      setError(
        cause instanceof ApiFailure ? cause.message : "Could not cancel that reservation.",
      );
    }
  }

  if (loading) {
    return <p className="empty">Loading the catalogue…</p>;
  }

  return (
    <>
      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}

      <section>
        <div className="section-head">
          <h2>Catalogue</h2>
          <div className="controls">
            <label>
              Borrowing as
              <select
                value={activeMemberId ?? ""}
                onChange={(event) => setActiveMemberId(Number(event.target.value))}
              >
                {members.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="toggle">
              <input
                type="checkbox"
                checked={onlyAvailable}
                onChange={(event) => setOnlyAvailable(event.target.checked)}
              />
              On the shelf only
            </label>
          </div>
        </div>
        <BookTable
          books={books}
          reservations={reservations.filter(
            (reservation) => reservation.memberId === activeMemberId,
          )}
          busyBookId={busyBookId}
          onBorrow={borrow}
          onReserve={reserve}
        />
      </section>

      <section>
        <div className="section-head">
          <h2>Out on loan</h2>
        </div>
        <LoanList loans={loans} onReturn={returnLoan} />
      </section>

      <section>
        <div className="section-head">
          <h2>Reserved</h2>
        </div>
        <ReservationList reservations={reservations} onCancel={cancelReservation} />
      </section>
    </>
  );
}
