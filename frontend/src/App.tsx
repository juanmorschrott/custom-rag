import { useState } from 'react'
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

function App() {
  const [query, setQuery] = useState('')
  const [response, setResponse] = useState<SearchResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

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

  return (
    <main className="app-shell">
      <header className="topbar">
        <span className="brand-mark">CR</span>
        <div><p className="eyebrow">PERSONAL KNOWLEDGE BASE</p><strong>Custom RAG</strong></div>
        <span className="status">Local library</span>
      </header>
      <section className="intro">
        <p className="eyebrow">BOOK NAVIGATOR</p>
        <h1>Find where a concept lives.</h1>
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
        <p className="hint">Your books will be indexed from <code>data/books/</code>.</p>
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
                  <b className="source-number">[{index + 1}]</b> <strong>{source.bookTitle}</strong>
                <span>{source.chapter}</span>
                <span>{source.pageStart === source.pageEnd ? `p. ${source.pageStart}` : `pp. ${source.pageStart}-${source.pageEnd}`}</span>
              </div>
              <p>{source.excerpt}</p>
            </article>
          )) : <p className="no-results">No matching passages were found in the indexed books.</p>}
        </section>
      ) : !loading && !error ? (
        <section className="empty-state">
          <span className="empty-icon">+</span>
          <div><h2>Your library is waiting</h2><p>Add PDFs to <code>data/books/</code>, then run the ingestion scan.</p></div>
        </section>
      ) : loading ? <p className="loading-state" role="status">Searching your indexed books...</p> : null}
    </main>
  )
}

export default App
