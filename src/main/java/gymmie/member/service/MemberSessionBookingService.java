package gymmie.member.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.Member;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.MembershipRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;

/** Books an eligible training session for the authenticated Member. */
public final class MemberSessionBookingService {
    private final MembershipRepository memberships;
    private final TrainingSessionRepository sessions;
    private final BookingRepository bookings;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the booking service with a clock for membership and session cut-offs. */
    public MemberSessionBookingService(MembershipRepository memberships, TrainingSessionRepository sessions,
            BookingRepository bookings, UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.memberships = Objects.requireNonNull(memberships);
        this.sessions = Objects.requireNonNull(sessions);
        this.bookings = Objects.requireNonNull(bookings);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Books a session if the Member, session, capacity and membership dates are eligible.
     *
     * @param sessionId session to book.
     * @return the persisted booking.
     * @throws Exception if authorization or persistence fails, or eligibility checks reject the booking.
     */
    public Booking book(long sessionId) throws Exception {
        return unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            LocalDate today = LocalDate.now(clock);
            LocalDateTime now = LocalDateTime.now(clock);
            Member member = new Member(account, memberships.findByMemberId(connection, account.id()));
            var membership = member.activeMembershipOn(today)
                    .orElseThrow(() -> new ConflictException("An active membership is required to book sessions"));
            TrainingSession trainingSession = sessions.findById(connection, sessionId)
                    .filter(candidate -> !candidate.cancelled())
                    .orElseThrow(() -> new ConflictException("This session is no longer available"));
            if (trainingSession.hasStartedAt(now)) {
                throw new ConflictException("This session has already started");
            }
            if (trainingSession.startsAt().toLocalDate().isAfter(membership.expiryDate())) {
                throw new ConflictException("Your membership expires before this session starts");
            }
            var existingBooking = bookings.findByMemberAndSession(connection, account.id(), sessionId);
            if (existingBooking.filter(booking -> booking.status() == BookingStatus.BOOKED).isPresent()) {
                throw new ConflictException("You have already booked this session");
            }
            if (bookings.countBookedBySessionId(connection, sessionId) >= trainingSession.capacity()) {
                throw new ConflictException("This session is full");
            }
            if (existingBooking.isPresent()) {
                Booking previous = existingBooking.orElseThrow();
                bookings.reactivate(connection, previous.id(), now);
                return new Booking(previous.id(), sessionId, account.id(), now, BookingStatus.BOOKED, null);
            }
            Booking booking = new Booking(bookings.nextId(connection), sessionId, account.id(), now,
                    BookingStatus.BOOKED, null);
            bookings.insert(connection, booking);
            return booking;
        });
    }
}
