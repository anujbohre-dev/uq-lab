# Two-minute demonstration

Prepare before the demo: the system is running (`docker compose up --build`, page at http://localhost:3000),
and one account already exists. Keep Swagger UI open in a second tab.

## 1. The problem (20 seconds)
"A steel column's buckling load depends on Young's modulus, diameter, length and end conditions. None is
known exactly. Instead of one number, this application gives the distribution of the answer."

## 2. Sign in (10 seconds)
Create an account or sign in. Say: "Passwords are stored as BCrypt hashes. Login returns a signed token."

## 3. Run a simulation (30 seconds)
Keep the Euler column model. Point at the four inputs and their distributions. Press **Run simulation**.
Say: "The job is queued and the server answers at once. The page polls, and the status moves from Queued
to Running to Done."

## 4. Read the result (30 seconds)
Show the mean, standard deviation, the histogram and the 5th and 95th percentiles. Say: "The mean alone
would hide that the load could be about twenty percent lower. Percentiles show the safe range."
Run again with the same seed to show the identical result: "The seed makes it reproducible, which is what
makes it testable."

## 5. Privacy (20 seconds)
Log out, create a second account, show the empty job list. Say: "Jobs belong to their owner. Another
user's job returns 404, so even its existence is not revealed."

## 6. Engineering (10 seconds)
Show Swagger UI, then the GitHub Actions tab. Say: "Each push runs the backend tests, the frontend tests
and a Docker build. The schema is versioned with Flyway on PostgreSQL."

## If something fails live
Say what it is and move on. The tests and CI are the evidence that it works.
