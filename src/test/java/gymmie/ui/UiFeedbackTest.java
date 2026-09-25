package gymmie.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import gymmie.model.exception.ConflictException;
import gymmie.model.exception.ValidationException;
import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthorizationException;

class UiFeedbackTest {
    @Test
    void expectedServiceErrorsKeepTheirExplanationAcrossAsyncBoundaries() {
        assertEquals("Choose a future date.", UiFeedback.errorMessage(
                new CompletionException(new ExecutionException(new ValidationException("Choose a future date"))),
                "Failed."));
        assertEquals("This plan is already archived.", UiFeedback.errorMessage(
                new ConflictException("This plan is already archived."), "Failed."));
        assertEquals("Trainer access required.", UiFeedback.errorMessage(
                new AuthorizationException("Trainer access required"), "Failed."));
        assertEquals("This account is deactivated. Please contact a Manager.",
                UiFeedback.errorMessage(new AccountDeactivatedException(), "Failed."));
    }

    @Test
    void unexpectedFailuresNeverExposeInfrastructureDetails() {
        assertEquals("Unable to save. Please try again.", UiFeedback.errorMessage(
                new SQLException("SQL and private database path"), "Unable to save. Please try again."));
        assertEquals("Failed.", UiFeedback.errorMessage(new ValidationException(" "), "Failed."));
    }
}
