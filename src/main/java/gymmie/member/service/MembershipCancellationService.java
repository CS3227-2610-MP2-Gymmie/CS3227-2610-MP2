package gymmie.member.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Member;
import gymmie.model.Membership;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.model.exception.ConflictException;
import gymmie.persistence.UnitOfWork;
import gymmie.persistence.repository.BookingRepository;
import gymmie.persistence.repository.MembershipRepository;
import gymmie.persistence.repository.TrainingSessionRepository;
import gymmie.service.Permissions;

/** Cancels the signed-in Member's current membership and future bookings atomically. */
public final class MembershipCancellationService {
    private final MembershipRepository memberships;
    private final BookingRepository bookings;
    private final TrainingSessionRepository sessions;
    private final UnitOfWork unitOfWork;
    private final Permissions permissions;
    private final Clock clock;

    /** Creates the cancellation service with a clock for membership and session cut-offs. */
    public MembershipCancellationService(MembershipRepository memberships, BookingRepository bookings,
            TrainingSessionRepository sessions, UnitOfWork unitOfWork, Permissions permissions, Clock clock) {
        this.memberships = Objects.requireNonNull(memberships);
        this.bookings = Objects.requireNonNull(bookings);
        this.sessions = Objects.requireNonNull(sessions);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.permissions = Objects.requireNonNull(permissions);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Cancels the displayed current membership and its future booked sessions.
     *
     * @param membershipId displayed membership identifier.
     * @return the number of future bookings cancelled.
     * @throws Exception if authorization or persistence fails, or the membership is no longer current.
     */
    public int cancel(long membershipId) throws Exception {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        return unitOfWork.inTransaction(connection -> {
            Account account = permissions.requireRole(connection, Role.MEMBER);
            Member member = new Member(account, memberships.findByMemberId(connection, account.id()));
            Membership current = member.activeMembershipOn(today)
                    .orElseThrow(() -> new ConflictException("There is no current membership to cancel"));
            if (current.id() != membershipId) {
                throw new ConflictException("This membership has changed. Refresh and try again.");
            }

            Membership cancelled = current.cancel();
            ArrayList<Membership> updatedHistory = new ArrayList<>(member.memberships());
            updatedHistory.replaceAll(membership -> membership.id() == cancelled.id() ? cancelled : membership);
            new Member(account, updatedHistory);
            memberships.update(connection, cancelled);

            int cancelledBookings = 0;
            for (Booking booking : bookings.findByMemberId(connection, account.id())) {
                if (booking.status() != BookingStatus.BOOKED) {
                    continue;
                }
                TrainingSession session = sessions.findById(connection, booking.sessionId()).orElseThrow();
                if (session.startsAt().isAfter(now)) {
                    bookings.update(connection, new Booking(booking.id(), booking.sessionId(), booking.memberId(),
                            booking.bookedAt(), BookingStatus.CANCELLED, CancellationReason.MEMBERSHIP_CANCELLED));
                    cancelledBookings++;
                }
            }
            return cancelledBookings;
        });
    }
}
