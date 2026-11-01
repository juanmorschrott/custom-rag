import assert from "node:assert/strict";
import { createServer } from "node:http";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

import { evaluateResponse, main, validateCases } from "./rag-eval.mjs";

test("validates a corpus-backed source and answer case", () => {
  assert.doesNotThrow(() =>
    validateCases({
      version: 1,
      cases: [
        {
          id: "chapter-source",
          query: "What is a vector database?",
          expectedSources: [{ bookTitleContains: "Database", chapterContains: "Vectors" }],
          answerContainsAny: ["embedding"],
        },
      ],
    }),
  );
});

test("rejects empty case lists and cases without assertions", () => {
  assert.throws(() => validateCases({ version: 1, cases: [] }), /No evaluation cases configured/);
  assert.throws(
    () => validateCases({ version: 1, cases: [{ id: "empty", query: "A question" }] }),
    /must define at least one assertion/,
  );
});

test("matches expected sources and answer terms case-insensitively", () => {
  const result = evaluateResponse(
    {
      expectedSources: [
        { bookTitleContains: "DATABASE", chapterContains: "vectors", pageStart: 42 },
      ],
      answerContainsAny: ["EMBEDDING"],
    },
    {
      answer: "An embedding represents the data as a vector [1].",
      results: [
        {
          bookTitle: "Database Systems",
          chapter: "Introduction to Vectors",
          pageStart: 42,
          pageEnd: 43,
        },
      ],
    },
  );

  assert.equal(result.passed, true);
  assert.equal(result.checks.length, 3);
});

test("fails when an expected source is missing", () => {
  const result = evaluateResponse(
    { expectedSources: [{ bookTitleContains: "Missing book" }] },
    { answer: "An answer.", results: [] },
  );

  assert.equal(result.passed, false);
  assert.equal(result.checks[0].passed, false);
});

test("checks no-source expectations", () => {
  const result = evaluateResponse(
    { expectNoSources: true },
    { answer: "No relevant passages.", results: [] },
  );

  assert.equal(result.passed, true);
});

test("rejects citations that do not refer to a returned source", () => {
  const result = evaluateResponse(
    { answerContainsAny: ["answer"] },
    { answer: "An answer [2].", results: [{ bookTitle: "Book" }] },
  );

  assert.equal(result.passed, false);
  assert.equal(result.checks[0].passed, false);
});

test("sends evaluation cases to the search API and reports success", async () => {
  const server = createServer(async (request, response) => {
    assert.equal(request.method, "POST");
    assert.equal(request.url, "/api/search");
    let body = "";
    for await (const chunk of request) body += chunk;
    assert.equal(JSON.parse(body).query, "What is a vector?");

    response.writeHead(200, { "content-type": "application/json" });
    response.end(
      JSON.stringify({
        answer: "A vector represents data [1].",
        results: [{ bookTitle: "Database Systems", chapter: "Vectors", pageStart: 42 }],
      }),
    );
  });
  const directory = await mkdtemp(join(tmpdir(), "custom-rag-eval-"));
  const casesPath = join(directory, "cases.json");
  await writeFile(
    casesPath,
    JSON.stringify({
      version: 1,
      cases: [
        {
          id: "vector-source",
          query: "What is a vector?",
          expectedSources: [{ bookTitleContains: "Database", pageStart: 42 }],
        },
      ],
    }),
  );

  try {
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    const address = server.address();
    assert.ok(address && typeof address === "object");
    const exitCode = await main([
      "--cases",
      casesPath,
      "--api-url",
      `http://127.0.0.1:${address.port}`,
    ]);
    assert.equal(exitCode, 0);
  } finally {
    await new Promise((resolve, reject) =>
      server.close((error) => (error ? reject(error) : resolve())),
    );
    await rm(directory, { recursive: true, force: true });
  }
});
