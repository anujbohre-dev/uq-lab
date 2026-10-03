# Phase 3: what changed

The backend is unchanged. Phase 3 adds the `frontend/` folder and updates the README.

## Changed
- `README.md` - status, tech stack, how to run the page, frontend design, limitations.

## New: project files in frontend/
- `package.json` - the list of JavaScript libraries (like `pom.xml`) and the commands `npm run dev`, `npm run build`, `npm test`.
- `package-lock.json` - the exact version of every library installed, so everyone gets identical code. Commit it.
- `vite.config.js` - the development server (port 5173), the forwarding of `/api` to port 8080, and the test setup.
- `index.html` - the single HTML page; React draws everything inside its `root` element.
- `.gitignore` - keeps `node_modules/` and `dist/` out of Git.

## New: frontend/src
- `main.jsx` - starts React and attaches `App` to the page.
- `App.jsx` - the page layout; loads the model list; holds the selected job.
- `index.css` - all styling, including the dark-mode colours and the phone layout.

### api/
- `client.js` - the four backend calls (models, submit job, get job, list jobs) and the `ApiError` class.

### lib/ (plain functions, no React)
- `format.js` - numbers, dates and durations as text; the list of statuses.
- `requestBuilder.js` - turns typed text into the request JSON and checks every value.
- `presets.js` - the example values that fill the form for each model.

### hooks/
- `usePolled.js` - loads data, reloads every two seconds while a job is unfinished, retries when the server is unreachable.

### components/
- `RunForm.jsx` - the new-simulation form and its submit logic.
- `DistributionField.jsx` - the controls for one input: distribution type and its numbers.
- `JobsPanel.jsx` - the job table with status filter and page buttons.
- `JobDetail.jsx` - the selected job: status, statistics, percentiles, histogram, request.
- `Histogram.jsx` - the bar chart, drawn as SVG.
- `StatusBadge.jsx` - the coloured status label.

### Tests (34)
- `test/setup.js` and `test/fakeBackend.js` - test set-up and an in-memory stand-in for the Java server.
- `lib/format.test.js` (6), `lib/requestBuilder.test.js` (7), `api/client.test.js` (6), `components/Histogram.test.jsx` (4), `App.test.jsx` (11).
