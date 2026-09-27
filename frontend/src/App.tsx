import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type SearchResult = {
  bookTitle: string
  chapter: string
  pageStart: number
  pageEnd: number
  excerpt: string
  relevance: number | null
}

type SearchResponse = {
  query: string
  answer: string
  results: SearchResult[]
}

type BookStatus = 'PENDING' | 'PROCESSING' | 'EXTRACTED' | 'INDEXED' | 'FAILED'

type CatalogBook = {
  id: string
  title: string
  sourceFilename: string
  status: BookStatus
  createdAt: string
}

type IngestionJobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED'

type IngestionJob = {
  id: string
  status: IngestionJobStatus
  totalFiles: number
  processedFiles: number
  indexedFiles: number
  skippedFiles: number
  failedFiles: number
  currentFilename: string | null
  lastError: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
}

const activeJobStorageKey = 'custom-rag-active-ingestion-job'

async function requestBooks(): Promise<CatalogBook[]> {
  const response = await fetch('/api/books')
  if (!response.ok) throw new Error(`Could not load books (${response.status}).`)
  return response.json() as Promise<CatalogBook[]>
}

function statusLabel(status: BookStatus | IngestionJobStatus) {
  return status.toLowerCase().replaceAll('_', ' ')
}

function App() {
  const [activeView, setActiveView] = useState<'search' | 'library'>('search')
  const [query, setQuery] = useState('')
  const [response, setResponse] = useState<SearchResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [books, setBooks] = useState<CatalogBook[]>([])
  const [booksLoading, setBooksLoading] = useState(true)
  const [libraryError, setLibraryError] = useState('')
  const [jobId, setJobId] = useState(() => window.localStorage.getItem(activeJobStorageKey))
  const [job, setJob] = useState<IngestionJob | null>(null)
  const [startingScan, setStartingScan] = useState(false)

  useEffect(() => {
    let cancelled = false
    requestBooks()
      .then((loadedBooks) => {
        if (!cancelled) setBooks(loadedBooks)
      })
      .catch((cause: unknown) => {
        if (!cancelled) setLibraryError(cause instanceof Error ? cause.message : 'Could not load books.')
      })
      .finally(() => {
        if (!cancelled) setBooksLoading(false)
      })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!jobId) return

    let cancelled = false
    let timer: number | undefined

    async function pollJob() {
      try {
        const result = await fetch(`/api/ingestion/jobs/${jobId}`)
        if (!result.ok) throw new Error(`Could not read scan status (${result.status}).`)
        const updatedJob = await result.json() as IngestionJob
        if (cancelled) return
        setJob(updatedJob)
        setLibraryError('')

        const finished = ['COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED'].includes(updatedJob.status)
        if (finished) {
          window.localStorage.removeItem(activeJobStorageKey)
          setJobId(null)
          requestBooks().then(setBooks).catch((cause: unknown) => {
            setLibraryError(cause instanceof Error ? cause.message : 'Could not refresh books.')
          })
          return
        }
        timer = window.setTimeout(pollJob, 3000)
      } catch (cause) {
        if (cancelled) return
        setLibraryError(cause instanceof Error ? cause.message : 'Could not read scan status.')
        timer = window.setTimeout(pollJob, 8000)
      }
    }

    void pollJob()
    return () => {
      cancelled = true
      if (timer !== undefined) window.clearTimeout(timer)
    }
  }, [jobId])

  async function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedQuery = query.trim()
    if (!normalizedQuery) return

    setLoading(true)
    setError('')
    setResponse(null)
    try {
      const result = await fetch('/api/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: normalizedQuery }),
      })
      if (!result.ok) throw new Error(`Search failed (${result.status}). Check that the backend and local models are available.`)
      setResponse(await result.json() as SearchResponse)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Search failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  async function startScan() {
    setStartingScan(true)
    setLibraryError('')
    try {
      const result = await fetch('/api/ingestion/jobs', { method: 'POST' })
      if (!result.ok) throw new Error(`Could not start scan (${result.status}).`)
      const startedJob = await result.json() as IngestionJob
      window.localStorage.setItem(activeJobStorageKey, startedJob.id)
      setJob(startedJob)
      setJobId(startedJob.id)
    } catch (cause) {
      setLibraryError(cause instanceof Error ? cause.message : 'Could not start scan.')
    } finally {
      setStartingScan(false)
    }
  }

  const scanActive = job?.status === 'QUEUED' || job?.status === 'RUNNING'
  const scanButtonDisabled = startingScan || scanActive || (jobId !== null && job === null)

  return (
    <main className="app-shell">
      <header className="topbar">
        <span className="brand-mark">CR</span>
        <div><p className="eyebrow">PERSONAL KNOWLEDGE BASE</p><strong>Custom RAG</strong></div>
        <nav className="view-nav" aria-label="Main navigation">
          <button type="button" className={activeView === 'search' ? 'active' : ''} aria-current={activeView === 'search' ? 'page' : undefined} onClick={() => setActiveView('search')}>Search</button>
          <button type="button" className={activeView === 'library' ? 'active' : ''} aria-current={activeView === 'library' ? 'page' : undefined} onClick={() => setActiveView('library')}>Library</button>
        </nav>
      </header>

      {activeView === 'search' ? (
        <>
          <section className="intro">
            <p className="eyebrow">BOOK NAVIGATOR</p>
            <h1>Find where a concept lives</h1>
            <p className="lede">Search your engineering library and get the book, chapter and page behind every result.</p>
          </section>
          <section className="search-panel" aria-label="Book search">
            <form onSubmit={search}>
              <label htmlFor="concept">What are you trying to understand?</label>
              <div className="search-row">
                <input id="concept" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="e.g. optimistic locking" />
                <button type="submit" disabled={loading || !query.trim()}>{loading ? 'Searching...' : 'Search library'}</button>
              </div>
            </form>
            <p className="hint">Your books are indexed from <code>data/books/</code>.</p>
          </section>
          {error && <p className="error-message" role="alert">{error}</p>}
          {response ? (
            <section className="results" aria-live="polite">
              <p className="eyebrow">ANSWER</p>
              <p className="answer-text">{response.answer}</p>
              <div className="source-heading"><h2>Sources</h2><span>{response.results.length} passages</span></div>
              {response.results.length ? response.results.map((source, index) => (
                <article className="source-result" key={`${source.bookTitle}-${source.pageStart}-${index}`}>
                  <div className="source-meta">
                    <b className="source-number">[{index + 1}]</b>
                    <strong>{source.bookTitle}</strong>
                    <span>{source.chapter}</span>
                    <span>{source.pageStart === source.pageEnd ? `p. ${source.pageStart}` : `pp. ${source.pageStart}-${source.pageEnd}`}</span>
                  </div>
                  <p>{source.excerpt}</p>
                </article>
              )) : <p className="no-results">No matching passages were found in the indexed books.</p>}
            </section>
          ) : loading ? <p className="loading-state" role="status">Searching your indexed books...</p> : null}
        </>
      ) : (
        <section className="library-view">
          <div className="library-heading">
            <div>
              <p className="eyebrow">LOCAL COLLECTION</p>
              <h1>Your library</h1>
              <p className="lede">PDFs in <code>data/books/</code>, including subfolders.</p>
            </div>
            <button className="scan-button" type="button" onClick={startScan} disabled={scanButtonDisabled}>
              {startingScan ? 'Starting...' : scanActive ? 'Scanning...' : 'Scan library'}
            </button>
          </div>

          {libraryError && <p className="error-message" role="alert">{libraryError}</p>}
          {job && (
            <section className="job-progress" aria-live="polite">
              <div className="job-summary">
                <strong className={`status-label status-${job.status.toLowerCase().replaceAll('_', '-')}`}>{statusLabel(job.status)}</strong>
                <span>{job.processedFiles} / {job.totalFiles} files</span>
              </div>
              <progress max={Math.max(job.totalFiles, 1)} value={job.processedFiles} />
              <div className="job-counts">
                <span>{job.indexedFiles} indexed</span><span>{job.skippedFiles} skipped</span><span>{job.failedFiles} failed</span>
              </div>
              {job.currentFilename && <p className="current-file">Processing <code>{job.currentFilename}</code></p>}
              {job.lastError && <p className="job-error">Latest error: {job.lastError}</p>}
            </section>
          )}

          <div className="library-list-heading">
            <h2>Books</h2>
            <span>{booksLoading ? 'Loading…' : `${books.length} total`}</span>
          </div>
          {booksLoading ? <p className="loading-state" role="status">Loading library...</p> : books.length ? (
            <div className="book-table-wrap">
              <table className="book-table">
                <thead><tr><th>Book</th><th>File</th><th>Status</th><th>Added</th></tr></thead>
                <tbody>
                  {books.map((book) => (
                    <tr key={book.id}>
                      <td>{book.title}</td>
                      <td><code>{book.sourceFilename}</code></td>
                      <td><span className={`status-label status-${book.status.toLowerCase()}`}>{statusLabel(book.status)}</span></td>
                      <td>{new Date(book.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <p className="no-results">No books indexed yet. Add PDFs to <code>data/books/</code> and scan the library.</p>}
        </section>
      )}
    </main>
  )
}

export default App
