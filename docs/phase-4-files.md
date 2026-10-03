# Phase 4: files and what they do

Phase 4 adds accounts. A person registers, signs in, receives a signed token (JWT), and the
browser sends it with every request. Jobs belong to the user who created them.

## Backend: new package `auth/`

- `AppUser`, `UserRepository`: the user table (`app_user`). The username is unique; only a BCrypt hash of the password is stored.
- `RegisterRequest`, `LoginRequest`, `TokenResponse`, `UserResponse`: the JSON shapes. The request records hide the password in `toString`.
- `UserService`: registers (lower-cases the name, refuses passwords over 72 bytes, handles two people choosing the same name at once) and checks logins (compares against a dummy hash for unknown names, so timing does not reveal which names exist).
- `TokenService`: builds and signs the JWT. The subject is the user id; the claim `username` carries the name; issuer `uq-lab`; 60 minutes.
- `SecurityConfig`: the rules. No session, no CSRF (no cookies are used), register/login/Swagger/health open, everything else needs a valid token. It also reads `UQLAB_JWT_SECRET`.
- `AuthController`: `/api/auth/register`, `/login`, `/me`.
- `UsernameTakenException`, `InvalidCredentialsException`, `InvalidRegistrationException`: mapped to 409, 401 and 400 in `ApiExceptionHandler`.
- `OpenApiConfig`: adds the Authorize button to Swagger UI.

## Backend: changed files

- `SimulationJob`, `JobStore`, `JobRepository`, `JpaJobStore`, `JobService`, `JobController`: jobs carry an `ownerId`; reads and lists are filtered by owner. Someone else's job gives `404`, not `403`, so job ids are not revealed.
- `SimulationController`: now requires a token.
- `pom.xml`: Spring Security, OAuth2 resource server (used only for its JWT support) and spring-security-test.
- `application.properties`: the secret and token lifetime. Test properties use a fixed test-only secret.

## Frontend

- `src/auth/session.js`: remembers the user in `sessionStorage` (cleared when the tab closes).
- `src/auth/validation.js`: the same rules as the backend, applied before sending.
- `src/auth/LoginPage.jsx`: Sign in and Create account.
- `src/api/client.js`: `register`, `login`, `getMe`, the `Authorization` header, and a handler that signs the user out when the server answers 401 to a request that carried a token.
- `src/App.jsx`: shows the sign-in page or the workspace; the header shows the username and **Log out**.
- Tests: `fakeBackend.js` now has accounts and refuses requests without a valid token.

## Tests

- Backend: 94 tests (66 before). New: `AuthControllerTest`, `SecurityRulesTest`, `SecurityConfigTest`; job tests now run as a signed-in user and check that users cannot see each other's jobs.
- Frontend: 50 tests (34 before).

## Honest note on verification

The frontend tests ran and passed. The backend security code was compiled here only against
stand-in copies of the library interfaces, because the real libraries could not be downloaded in
the build environment. Its first real compile and test run is on your laptop in IntelliJ.

## Concepts to be able to explain

- Hashing vs encryption: a password is hashed (one way, salted, slow) with BCrypt, never stored or decrypted.
- A JWT is a signed statement ("this is user X until time T"); anyone can read it, only the server can sign it. So never put secrets in it.
- Stateless: the server keeps no session; it re-verifies the signature on each request.
- Authentication (who are you) vs authorization (what may you touch): login vs the owner filter on jobs.
- Why the same error for wrong password and unknown user.
