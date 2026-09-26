CREATE TABLE book_chunks (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters (id),
    ordinal INTEGER NOT NULL,
    page_start INTEGER NOT NULL,
    page_end INTEGER NOT NULL,
    content TEXT NOT NULL,
    CONSTRAINT uq_book_chunks_book_ordinal UNIQUE (book_id, ordinal)
);

CREATE INDEX idx_book_chunks_book_id ON book_chunks (book_id);
CREATE INDEX idx_book_chunks_chapter_id ON book_chunks (chapter_id);