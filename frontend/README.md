# Frontend

The React and TypeScript UI is built with Vite. Its source is under `src/`; the app is served by Docker Compose on port 3000 and calls the backend API on the same origin through the configured web server.

From this directory:

- `npm run dev` starts the Vite development server.
- `npm run build` runs the TypeScript project build and creates the production bundle.
- `npm run lint` runs ESLint.

See the [root README](../README.md), [agent instructions](../AGENTS.md), and [product specification](../specs/product.md) for whole-project setup and behavior.
