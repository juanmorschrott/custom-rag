CREATE TABLE books (
    id UUID PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    author VARCHAR(300),
    source_filename VARCHAR(500) NOT NULL,
    checksum VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chapters (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books (id),
    chapter_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    page_start INTEGER,
    page_end INTEGER,
    CONSTRAINT uq_chapters_book_number UNIQUE (book_id, chapter_number)
);

CREATE INDEX idx_chapters_book_id ON chapters (book_id);
CREATE INDEX idx_books_status ON books (status);
