package gymmie.trainer;

import java.util.Optional;

import gymmie.trainer.service.SessionCancellationService.Preview;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.SharedStyles;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Shows the session, current bookings and a required reason before explicit confirmation. */
final class SessionCancellationDialog {
    private SessionCancellationDialog() {
    }

    static Optional<String> confirm(Window owner, Preview preview) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Cancel session");
        dialog.setHeaderText("Review session cancellation");
        dialog.setResizable(true);
        var session = preview.session();
        Label details = new Label("Session #" + session.id() + " · "
                + DisplayFormatters.dateTime(session.startsAt()) + "\n" + session.durationMinutes()
                + " minutes · Capacity: " + session.capacity() + "\n"
                + (session.description() == null ? "" : session.description()));
        details.setWrapText(true);
        VBox bookingList = new VBox(6);
        for (int index = 0; index < preview.bookings().size(); index++) {
            Label booking = new Label("Booking #" + preview.bookings().get(index).id()
                    + " · " + preview.memberNames().get(index));
            booking.setWrapText(true);
            bookingList.getChildren().add(booking);
        }
        ScrollPane scroll = new ScrollPane(bookingList);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(140);
        TextArea reason = new TextArea();
        reason.setId("sessionCancellationReason");
        reason.setWrapText(true);
        reason.setPrefRowCount(3);
        Label reasonLabel = new Label("Reason for cancellation (required, visible to affected Members)");
        reasonLabel.setWrapText(true);
        reasonLabel.setLabelFor(reason);
        reason.setAccessibleText("Reason for cancellation, required");
        // Tab leaves the multi-line reason field so both dialog actions remain keyboard reachable.
        reason.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.TAB && !event.isControlDown()
                    && !event.isAltDown() && !event.isMetaDown()) {
                dialog.getDialogPane().lookupButton(ButtonType.CANCEL).requestFocus();
                event.consume();
            }
        });
        Label warning = new Label("The session and all current bookings will be cancelled. "
                + "Existing cancelled bookings keep their original reasons. This cannot be undone.");
        warning.setWrapText(true);
        VBox content = new VBox(12, details,
                new Label("Current bookings: " + preview.bookings().size()), scroll, reasonLabel, reason, warning);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(540);
        ButtonType confirm = new ButtonType("Confirm cancellation", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, confirm);
        dialog.getDialogPane().lookupButton(confirm).setId("confirmSessionCancellation");
        dialog.getDialogPane().lookupButton(confirm).disableProperty()
                .bind(Bindings.createBooleanBinding(() -> reason.getText().isBlank(), reason.textProperty()));
        SharedStyles.apply(dialog.getDialogPane());
        dialog.setOnShown(_ -> reason.requestFocus());
        return dialog.showAndWait().filter(confirm::equals).map(_ -> reason.getText());
    }
}
