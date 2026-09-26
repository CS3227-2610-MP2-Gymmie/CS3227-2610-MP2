package gymmie.member.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import gymmie.model.Account;
import gymmie.model.BookingStatus;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.AccountRepository;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;

/** Reads upcoming sessions and current booking counts for the authenticated Member. */
public final class MemberSessionBrowseService {
    private final AccountRepository accounts;
    private final TrainingSessionRepository sessions;
    private final BookingRepository bookings;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the Member session reader with its persistence and authorization dependencies. */
    public MemberSessionBrowseService(AccountRepository accounts, TrainingSessionRepository sessions,
            BookingRepository bookings, UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.accounts = Objects.requireNonNull(accounts);
        this.sessions = Objects.requireNonNull(sessions);
        this.bookings = Objects.requireNonNull(bookings);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Lists future, uncancelled sessions owned by active Trainers with current booking counts.
     *
     * @return sessions ordered by start time, Trainer name, then session identifier.
     * @throws Exception if the caller is not an active Member or persistence fails.
     */
    public List<BrowseSession> upcomingSessions() throws Exception {
        return unitOfWork.inTransaction(connection -> {
            Account member = permissions.requireRole(connection, Role.MEMBER);
            Map<Long, Account> activeTrainers = accounts.findAllActive(connection).stream()
                    .filter(account -> account.role() == Role.TRAINER)
                    .collect(Collectors.toMap(Account::id, Function.identity()));
            LocalDateTime now = LocalDateTime.now(clock);
            List<BrowseSession> upcoming = new ArrayList<>();
            for (TrainingSession session : sessions.findUpcoming(connection, now)) {
                Account trainer = activeTrainers.get(session.trainerId());
                if (trainer != null) {
                    upcoming.add(toBrowseSession(session, trainer,
                            bookings.countBookedBySessionId(connection, session.id()),
                            bookings.findByMemberAndSession(connection, member.id(), session.id())
                                    .filter(booking -> booking.status() == BookingStatus.BOOKED)
                                    .isPresent()));
                }
            }
            upcoming.sort(Comparator.comparing(BrowseSession::startsAt)
                    .thenComparing(BrowseSession::trainerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(BrowseSession::sessionId));
            return List.copyOf(upcoming);
        });
    }

    private static BrowseSession toBrowseSession(TrainingSession session, Account trainer, int bookingCount,
            boolean hasBooking) {
        return new BrowseSession(session.id(), trainer.id(), trainer.displayName(), session.startsAt(),
                session.durationMinutes(), session.description(), session.capacity(), bookingCount, hasBooking);
    }

    /**
     * Credential-free session details displayed to Members.
     *
     * @param sessionId session identifier.
     * @param trainerId owning Trainer account identifier.
     * @param trainerName Trainer's current display name.
     * @param startsAt local session start time.
     * @param durationMinutes session duration.
     * @param description optional session description.
     * @param capacity maximum number of Members.
     * @param bookingCount current number of booked Members.
     * @param hasBooking whether the signed-in Member has an active booking for this session.
     */
    public record BrowseSession(long sessionId, long trainerId, String trainerName,
            LocalDateTime startsAt, int durationMinutes, String description, int capacity, int bookingCount,
            boolean hasBooking) {
    }
}
