package com.anujbohre.uqlab.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Registers users and checks logins. Passwords are only ever handled as BCrypt hashes. */
@Service
public class UserService {

    /** BCrypt reads at most 72 bytes of a password, so longer ones are refused instead of cut. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /**
     * A hash of a throw-away password. Login compares against it when the username does not exist,
     * so that "no such user" takes about as long as "wrong password" and timing reveals nothing.
     */
    private final String dummyHash;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** Creates an account. Throws {@link UsernameTakenException} if the name is already used. */
    public AppUser register(String rawUsername, String password) {
        String username = normalise(rawUsername);
        if (tooLong(password)) {
            throw new InvalidRegistrationException("password must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        AppUser user = new AppUser(
                UUID.randomUUID().toString(), username, passwordEncoder.encode(password), clock.instant());
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two people registering the same name at the same moment: the database's unique
            // constraint decides, and the loser is told the name is taken.
            throw new UsernameTakenException(username);
        }
    }

    /** Returns the user if the password is right. Throws {@link InvalidCredentialsException} otherwise. */
    public AppUser authenticate(String rawUsername, String password) {
        Optional<AppUser> found = users.findByUsername(normalise(rawUsername));
        String hash = found.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean passwordOk = !tooLong(password) && passwordEncoder.matches(password, hash);
        if (found.isEmpty() || !passwordOk) {
            throw new InvalidCredentialsException();
        }
        return found.get();
    }

    private static String normalise(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean tooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }
}
