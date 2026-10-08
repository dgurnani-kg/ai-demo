INSERT INTO book (isbn, title, author, total_copies) VALUES
    ('978-0132350884', 'Clean Code',                        'Robert C. Martin', 3),
    ('978-0201633610', 'Design Patterns',                    'Erich Gamma',      2),
    ('978-0134685991', 'Effective Java',                     'Joshua Bloch',     4),
    ('978-1491950357', 'Building Microservices',             'Sam Newman',       1),
    ('978-0321125217', 'Domain-Driven Design',               'Eric Evans',       2);

INSERT INTO library_member (name, email) VALUES
    ('Priya Raman',   'priya.raman@example.com'),
    ('Tomas Lindgren','tomas.lindgren@example.com'),
    ('Aisha Okonkwo', 'aisha.okonkwo@example.com');

-- One copy of Building Microservices is already out, so availability is visibly non-trivial.
INSERT INTO loan (book_id, member_id, borrowed_at, due_at) VALUES
    (4, 1, TIMESTAMP WITH TIME ZONE '2026-09-20 09:00:00+00', TIMESTAMP WITH TIME ZONE '2026-10-11 09:00:00+00');
