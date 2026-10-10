-- Building Microservices is fully out on loan, so Tomas is already waiting in the queue
-- for it. Without this row the reservations screen is empty on first load.
INSERT INTO reservation (book_id, member_id, status, reserved_at, ready_at) VALUES
    (4, 2, 'WAITING', TIMESTAMP WITH TIME ZONE '2026-09-25 10:00:00+00', NULL);
