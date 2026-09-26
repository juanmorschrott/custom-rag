COMPOSE ?= docker compose
API_URL ?= http://localhost:8080
QUERY ?=
export QUERY

.PHONY: up models down index-books search

up:
	$(COMPOSE) up --build -d

models:
	$(COMPOSE) exec ollama ollama pull embeddinggemma:latest
	$(COMPOSE) exec ollama ollama pull phi4:latest

down:
	$(COMPOSE) down

index-books:
	curl --fail-with-body --silent --show-error -X POST "$(API_URL)/api/ingestion/scan" | jq .

search:
	@test -n "$$QUERY" || { echo 'Usage: make search QUERY="your question"'; exit 2; }
	@curl --fail-with-body --silent --show-error \
		-H 'Content-Type: application/json' \
		-d "$$(jq -cn --arg query "$$QUERY" '{query:$$query}')" \
		"$(API_URL)/api/search" | jq .