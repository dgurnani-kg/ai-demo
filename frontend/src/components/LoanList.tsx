import type { Loan } from "../api/client";

type Props = {
  loans: Loan[];
  onReturn: (loanId: number) => void;
};

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function LoanList({ loans, onReturn }: Props) {
  const open = loans.filter((loan) => !loan.returnedAt);

  if (open.length === 0) {
    return <p className="empty">No books are out.</p>;
  }

  return (
    <ul className="loans">
      {open.map((loan) => (
        <li key={loan.id}>
          <div>
            <strong>{loan.bookTitle}</strong>
            <span className="meta">
              {loan.memberName} · due {formatDate(loan.dueAt)}
            </span>
          </div>
          <button type="button" onClick={() => onReturn(loan.id)}>
            Return
          </button>
        </li>
      ))}
    </ul>
  );
}
