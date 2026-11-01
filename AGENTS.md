# Project Guidelines

## Project

Custom RAG is a local-first application for searching technical books in PDF form. The React/TypeScript frontend is in `frontend/`; the Spring Boot/Java API is in `backend/`. PostgreSQL with pgvector stores catalog data and embeddings, and Ollama provides embeddings and answer generation. See [the architecture overview](docs/architecture.md) for component and data-flow details, and [the product specification](specs/product.md) for current behavior.

## Development

- Start the stack with `docker compose up --build -d`; prepare Ollama with `make models`.
- Backend tests: `cd backend && ./mvnw test`.
- Frontend checks: `cd frontend && npm run build && npm run lint`.
- RAG evaluation: follow [the evaluation guide](specs/rag-evaluation.md) and run `make rag-eval` after configuring cases and indexing books.
- Keep changes focused and preserve existing API response shapes and source metadata used by the UI.

## Conventions

- Add database changes as new Flyway migrations under `backend/src/main/resources/db/migration/`; do not edit migrations that may already have been applied.
- Keep backend packages organized by feature (`catalog`, `ingestion`, `search`) and frontend components/types in the existing `frontend/src` structure.
- Do not commit book PDFs, credentials, local environment files, or generated build output.
- Update the relevant specification and documentation when intentionally changing observable behavior.
