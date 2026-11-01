# RAG evaluation

The evaluation harness checks the running search API against expectations grounded in books that have actually been indexed. It uses only Node.js built-ins and does not add project dependencies.

For searches that return sources, it also checks that the answer cites at least one returned passage using a valid numbered citation.

## Prepare cases

Edit [`rag-evaluation.json`](rag-evaluation.json). Add one case per question, using source details that can be verified in the indexed books:

```json
{
  "id": "concept-from-a-book",
  "query": "What does this book say about the concept?",
  "expectedSources": [
    {
      "bookTitleContains": "Book title",
      "chapterContains": "Chapter name",
      "pageStart": 42
    }
  ],
  "answerContainsAny": ["a key term", "another accepted term"]
}
```

Each case must include at least one assertion:

- `expectedSources`: every entry must match a returned result. Book and chapter checks are case-insensitive substring matches; page checks, when provided, are exact.
- `answerContainsAny`: at least one term must appear in the generated answer (case-insensitive).
- `expectNoSources: true`: use for a query that should not be answerable from the indexed corpus; the API must return no results.

Do not invent expected facts or page numbers. Keep the case set small and stable, and revise it when the corpus or product behavior intentionally changes. Model-generated wording is variable, so prefer a few meaningful answer terms over exact answer snapshots.

## Run

1. Start the Docker Compose stack and pull the models (`docker compose up --build -d`, then `make models`).
2. Put the evaluation books under `data/books/` and index them using **Scan library** or `make index-books`.
3. Add cases to `specs/rag-evaluation.json`.
4. Run `make rag-eval`. Set `API_URL` to target a different backend, for example `make rag-eval API_URL=http://localhost:8081`.

The command exits non-zero if a case fails, the API is unavailable, or no cases are configured. The report shows each case and a pass/fail summary. This is an integration/evaluation check, not a replacement for the backend unit tests.
