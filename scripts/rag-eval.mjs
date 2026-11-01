#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";

function usage() {
  return "Usage: node scripts/rag-eval.mjs [--cases PATH] [--api-url URL]";
}

function parseArgs(args) {
  const options = {
    cases: "specs/rag-evaluation.json",
    apiUrl: process.env.API_URL ?? "http://localhost:8080",
  };

  for (let index = 0; index < args.length; index += 1) {
    const option = args[index];
    if (option === "--help" || option === "-h") {
      return { help: true };
    }
    if (option !== "--cases" && option !== "--api-url") {
      throw new Error(`Unknown option: ${option}\n${usage()}`);
    }

    const value = args[index + 1];
    if (!value || value.startsWith("--")) {
      throw new Error(`Missing value for ${option}\n${usage()}`);
    }
    if (option === "--cases") options.cases = value;
    if (option === "--api-url") options.apiUrl = value;
    index += 1;
  }

  return options;
}

function normalize(value) {
  return String(value ?? "").toLowerCase();
}

function matchesSource(expected, actual) {
  if (
    expected.bookTitleContains &&
    !normalize(actual.bookTitle).includes(normalize(expected.bookTitleContains))
  ) {
    return false;
  }
  if (
    expected.chapterContains &&
    !normalize(actual.chapter).includes(normalize(expected.chapterContains))
  ) {
    return false;
  }
  if (expected.pageStart !== undefined && actual.pageStart !== expected.pageStart) {
    return false;
  }
  if (expected.pageEnd !== undefined && actual.pageEnd !== expected.pageEnd) {
    return false;
  }
  return true;
}

export function validateCases(configuration) {
  if (configuration?.version !== 1 || !Array.isArray(configuration.cases)) {
    throw new Error("Evaluation cases must be a version 1 JSON object with a cases array.");
  }
  if (configuration.cases.length === 0) {
    throw new Error(
      "No evaluation cases configured. Add corpus-backed cases to specs/rag-evaluation.json; see specs/rag-evaluation.md.",
    );
  }

  const ids = new Set();
  for (const [index, testCase] of configuration.cases.entries()) {
    const location = `cases[${index}]`;
    if (!testCase || typeof testCase.id !== "string" || !testCase.id.trim()) {
      throw new Error(`${location} must have a non-empty id.`);
    }
    if (ids.has(testCase.id)) {
      throw new Error(`Duplicate evaluation case id: ${testCase.id}`);
    }
    ids.add(testCase.id);

    if (typeof testCase.query !== "string" || !testCase.query.trim()) {
      throw new Error(`${location} (${testCase.id}) must have a non-empty query.`);
    }
    if (
      testCase.expectedSources !== undefined &&
      (!Array.isArray(testCase.expectedSources) ||
        testCase.expectedSources.some(
          (source) =>
            !source ||
            typeof source !== "object" ||
            !["bookTitleContains", "chapterContains", "pageStart", "pageEnd"].some(
              (key) => source[key] !== undefined,
            ) ||
            ["bookTitleContains", "chapterContains"].some(
              (key) => source[key] !== undefined &&
                (typeof source[key] !== "string" || !source[key].trim()),
            ) ||
            ["pageStart", "pageEnd"].some(
              (key) => source[key] !== undefined &&
                (!Number.isInteger(source[key]) || source[key] < 1),
            ),
        ))
    ) {
      throw new Error(`${location} (${testCase.id}) has an invalid expectedSources list.`);
    }
    if (
      testCase.answerContainsAny !== undefined &&
      (!Array.isArray(testCase.answerContainsAny) ||
        testCase.answerContainsAny.some((term) => typeof term !== "string" || !term.trim()))
    ) {
      throw new Error(`${location} (${testCase.id}) has an invalid answerContainsAny list.`);
    }
    if (testCase.expectNoSources !== undefined && typeof testCase.expectNoSources !== "boolean") {
      throw new Error(`${location} (${testCase.id}) expectNoSources must be a boolean.`);
    }
    const hasAssertions =
      (testCase.expectedSources?.length ?? 0) > 0 ||
      (testCase.answerContainsAny?.length ?? 0) > 0 ||
      testCase.expectNoSources === true;
    if (!hasAssertions) {
      throw new Error(`${location} (${testCase.id}) must define at least one assertion.`);
    }
  }
}

export function evaluateResponse(testCase, response) {
  if (typeof response?.answer !== "string" || !Array.isArray(response.results)) {
    throw new Error("Search API response must contain a string answer and a results array.");
  }
  if (response.results.some((result) => !result || typeof result !== "object")) {
    throw new Error("Search API response results must contain source objects.");
  }

  const checks = [];
  if (response.results.length > 0) {
    const citations = [...response.answer.matchAll(/\[(\d+)\]/g)].map((match) =>
      Number(match[1]),
    );
    checks.push({
      passed:
        citations.length > 0 &&
        citations.every((citation) => citation > 0 && citation <= response.results.length),
      description: "answer cites at least one returned source using a valid passage number",
    });
  }

  for (const expected of testCase.expectedSources ?? []) {
    const matched = response.results.some((actual) => matchesSource(expected, actual));
    checks.push({
      passed: matched,
      description: `expected source ${JSON.stringify(expected)}`,
    });
  }

  if ((testCase.answerContainsAny?.length ?? 0) > 0) {
    const answer = normalize(response.answer);
    const matched = testCase.answerContainsAny.some((term) =>
      answer.includes(normalize(term)),
    );
    checks.push({
      passed: matched,
      description: `answer contains any of ${JSON.stringify(testCase.answerContainsAny)}`,
    });
  }

  if (testCase.expectNoSources === true) {
    checks.push({
      passed: response.results.length === 0,
      description: "no source results returned",
    });
  }

  return {
    passed: checks.every((check) => check.passed),
    checks,
  };
}

async function loadConfiguration(path) {
  let contents;
  try {
    contents = await readFile(path, "utf8");
  } catch (error) {
    throw new Error(`Cannot read evaluation cases at ${path}: ${error.message}`, { cause: error });
  }

  let configuration;
  try {
    configuration = JSON.parse(contents);
  } catch (error) {
    throw new Error(`Invalid JSON in ${path}: ${error.message}`, { cause: error });
  }
  validateCases(configuration);
  return configuration;
}

async function runCase(testCase, apiUrl) {
  let response;
  try {
    response = await fetch(new URL("/api/search", apiUrl), {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ query: testCase.query }),
    });
  } catch (error) {
    throw new Error(`Could not reach search API at ${apiUrl}: ${error.message}`, { cause: error });
  }
  if (!response.ok) {
    const details = await response.text();
    throw new Error(`Search API returned ${response.status}: ${details}`);
  }

  let payload;
  try {
    payload = await response.json();
  } catch (error) {
    throw new Error(`Search API returned invalid JSON: ${error.message}`, { cause: error });
  }
  return evaluateResponse(testCase, payload);
}

export async function main(args = process.argv.slice(2)) {
  const options = parseArgs(args);
  if (options.help) {
    console.log(usage());
    return 0;
  }

  const configuration = await loadConfiguration(options.cases);
  let passedCases = 0;
  let passedChecks = 0;
  let totalChecks = 0;

  for (const testCase of configuration.cases) {
    const result = await runCase(testCase, options.apiUrl);
    const checkCount = result.checks.filter((check) => check.passed).length;
    totalChecks += result.checks.length;
    passedChecks += checkCount;
    if (result.passed) passedCases += 1;

    console.log(`${result.passed ? "PASS" : "FAIL"} ${testCase.id} (${checkCount}/${result.checks.length} checks)`);
    for (const check of result.checks.filter((item) => !item.passed)) {
      console.log(`  - ${check.description}`);
    }
  }

  console.log(
    `RAG evaluation: ${passedCases}/${configuration.cases.length} cases passed; ${passedChecks}/${totalChecks} checks passed.`,
  );
  return passedCases === configuration.cases.length ? 0 : 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().then(
    (exitCode) => {
      process.exitCode = exitCode;
    },
    (error) => {
      console.error(`RAG evaluation failed: ${error.message}`);
      process.exitCode = 1;
    },
  );
}
