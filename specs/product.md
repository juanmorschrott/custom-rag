# Product specification

**Status:** describes the behavior currently implemented; update this document when behavior changes intentionally.

## Purpose

Help readers find explanations of software concepts in their own technical PDF books, with source passages and book, chapter, and page references they can verify.

## User journeys

### Add books to the searchable library

1. The reader places PDF files under `data/books/` (nested folders are supported).
2. The reader starts a library scan from the UI.
3. The application extracts text page by page, uses PDF bookmarks as chapter boundaries when present, splits text into overlapping chunks, and indexes those chunks.
4. The UI shows scan progress and per-book indexing status.

### Search the books

1. The reader submits a question in the UI.
2. The API retrieves relevant passages from the indexed books and asks the language model to answer using those passages only.
3. The UI displays the answer and the retrieved passages with their book, chapter, and page references.
4. If there are no relevant passages, the application reports that it could not find relevant passages rather than generating an answer from unsupported context.

## Acceptance criteria

- A successful search response contains the original query, an answer, and a list of source results.
- Each source result identifies its book title, chapter, page range, excerpt, and relevance score.
- The generated answer uses the question's language and cites supporting passages by their result number.
- The reader can inspect source passages independently of the generated answer.
- Ingestion and search run locally using the services in Docker Compose; book PDFs remain on the host and are not committed to the repository.

## Constraints and non-goals

- Image-only/scanned PDFs are not OCR-processed and may yield no searchable text.
- Chapter detection uses PDF bookmarks when available; without them, content may be assigned to an unclassified range.
- The language model has no direct database access; retrieval is performed by the backend before generation.
- This specification does not promise exact answer wording or a particular retrieval ranking for every query.

## Related documentation

- [Architecture](../docs/architecture.md) describes services, storage, and data flow.
- [RAG evaluation](rag-evaluation.md) describes how to check retrieval and answer behavior against a local book corpus.
