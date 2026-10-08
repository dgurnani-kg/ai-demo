import { useCallback, useEffect, useState } from "react";
import { api, ApiFailure, type Book, type Loan, type Member } from "../api/client";
import BookTable from "../components/BookTable";
import LoanList from "../components/LoanList";

export default function CataloguePage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [activeMemberId, setActiveMemberId] = useState<number | null>(null);
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [busyBookId, setBusyBookId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const [nextBooks, nextLoans] = await Promise.all([
        api.listBooks(onlyAvailable),
        api.listLoans(),
      ]);
      setBooks(nextBooks);
      setLoans(nextLoans);
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

  async function returnLoan(loanId: number) {
    try {
      await api.returnLoan(loanId);
      await refresh();
    } catch (cause) {
      setError(cause instanceof ApiFailure ? cause.message : "Could not return that loan.");
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
        <BookTable books={books} busyBookId={busyBookId} onBorrow={borrow} />
      </section>

      <section>
        <div className="section-head">
          <h2>Out on loan</h2>
        </div>
        <LoanList loans={loans} onReturn={returnLoan} />
      </section>
    </>
  );
}
