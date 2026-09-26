package gymmie.trainer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gymmie.model.BookingStatus;
import gymmie.model.Role;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;
import gymmie.service.exception.AuthorizationException;

/** Provides display-name-only rosters after verifying Trainer role and session ownership. */
public final class SessionRosterService {
    private final TrainingSessionRepository sessions;
    private final BookingRepository bookings;
    private final AccountRepository accounts;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;

    /** Creates the roster authorization and persistence boundary. */
    public SessionRosterService(TrainingSessionRepository sessions, BookingRepository bookings,
            AccountRepository accounts, UnitOfWork unitOfWork, Permissions permissions) {
        this.sessions = Objects.requireNonNull(sessions);
        this.bookings = Objects.requireNonNull(bookings);
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
    }

    /**
     * Lists current bookings for a session owned by the authenticated Trainer.
     *
     * @param sessionId selected session identifier.
     * @return Member display names in alphabetical order, retaining duplicate names.
     * @throws Exception if authorization or persistence fails.
     */
    public List<String> getRoster(long sessionId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            permissions.requireRole(connection, Role.TRAINER);
            var session = sessions.findById(connection, sessionId)
                    .orElseThrow(() -> new AuthorizationException("Session is unavailable"));
            permissions.requireOwner(connection, Role.TRAINER, session.trainerId());
            List<String> names = new ArrayList<>();
            for (var booking : bookings.findBySessionId(connection, session.id())) {
                if (booking.status() == BookingStatus.BOOKED) {
                    var member = accounts.findById(connection, booking.memberId()).orElseThrow();
                    names.add(member.displayName());
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder()));
            return List.copyOf(names);
        });
    }
}
