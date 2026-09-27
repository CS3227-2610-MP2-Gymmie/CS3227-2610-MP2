package gymmie.manager;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.model.MembershipPlan;
import gymmie.model.exception.ValidationException;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for the Manager membership plan management screen.
 *
 * <p>
 * Delegates all catalogue operations to
 * {@link gymmie.manager.service.MembershipPlanService}.
 * Supports creating, editing, archiving, restoring, and deleting membership
 * plans through
 * mouse and keyboard interactions.
 */
public final class ManagerPlansController {
    private final AppContext context;
    private final Router router;
    private MembershipPlan editingPlan;

    @FXML
    private Button backButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Label formHeading;
    @FXML
    private TextField planName;
    @FXML
    private TextField planDuration;
    @FXML
    private TextField planPrice;
    @FXML
    private Button savePlanButton;
    @FXML
    private Button cancelEditButton;
    @FXML
    private VBox planList;
    @FXML
    private StatusLabel status;

    /**
     * Creates a controller using the application context and router.
     *
     * @param context application context containing services.
     * @param router  navigation coordinator.
     */
    public ManagerPlansController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        refresh();
        Platform.runLater(backButton::requestFocus);
    }

    @FXML
    private void refresh() {
        refresh(null);
    }

    private void refresh(Runnable onDone) {
        if (refreshButton.isDisabled()) {
            return;
        }
        refreshButton.setDisable(true);
        status.info("Loading plans…");
        Task<List<MembershipPlan>> task = new Task<>() {
            @Override
            protected List<MembershipPlan> call() throws Exception {
                return context.getMembershipPlanService().getAllPlans();
            }
        };
        task.setOnSucceeded(_ -> {
            refreshButton.setDisable(false);
            planList.getChildren().clear();
            List<MembershipPlan> plans = task.getValue();
            for (MembershipPlan plan : plans) {
                planList.getChildren().add(createPlanCard(plan));
            }
            if (onDone != null) {
                onDone.run();
            } else {
                status.info(plans.isEmpty() ? "No membership plans found." : "Plans loaded.");
            }
        });
        task.setOnFailed(_ -> {
            refreshButton.setDisable(false);
            status.error(task.getException(), "Unable to load plans. Please try Refresh again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-manager-plans").start(task);
    }

    @FXML
    private void savePlan() {
        if (savePlanButton.isDisabled()) {
            return;
        }
        String name = planName.getText();
        String durationStr = planDuration.getText();
        String priceStr = planPrice.getText();

        if (name == null || name.isBlank()) {
            status.error("Plan name must not be blank.");
            planName.requestFocus();
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr.strip());
        } catch (Exception exception) {
            status.error("Plan duration must be a valid number of days.");
            planDuration.requestFocus();
            return;
        }

        int priceCents;
        try {
            priceCents = parsePriceCents(priceStr);
        } catch (ValidationException exception) {
            status.error(exception.getMessage());
            planPrice.requestFocus();
            return;
        }

        savePlanButton.setDisable(true);
        boolean isEdit = editingPlan != null;
        long targetId = isEdit ? editingPlan.id() : 0;
        status.info(isEdit ? "Updating plan…" : "Creating plan…");

        Task<MembershipPlan> task = new Task<>() {
            @Override
            protected MembershipPlan call() throws Exception {
                if (isEdit) {
                    return context.getMembershipPlanService().edit(targetId, name, duration, priceCents);
                }
                return context.getMembershipPlanService().create(name, duration, priceCents);
            }
        };
        task.setOnSucceeded(_ -> {
            savePlanButton.setDisable(false);
            resetForm();
            refresh(() -> status.success(isEdit ? "Plan updated." : "Plan created."));
        });
        task.setOnFailed(_ -> {
            savePlanButton.setDisable(false);
            status.error(task.getException(), "Unable to save plan. Please check inputs and try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-save-plan").start(task);
    }

    @FXML
    private void cancelEdit() {
        resetForm();
        status.info("Edit cancelled.");
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            status.error("Unable to return to dashboard.");
        }
    }

    /**
     * Populates the plan form to edit an existing unarchived plan.
     *
     * @param plan existing plan to edit.
     */
    public void startEdit(MembershipPlan plan) {
        editingPlan = plan;
        formHeading.setText("Edit plan: " + plan.name());
        planName.setText(plan.name());
        planDuration.setText(String.valueOf(plan.durationDays()));
        planPrice.setText(String.valueOf(plan.priceCents()));
        savePlanButton.setText("_Save changes");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        planName.requestFocus();
    }

    private void resetForm() {
        editingPlan = null;
        formHeading.setText("Create new plan");
        planName.clear();
        planDuration.clear();
        planPrice.clear();
        savePlanButton.setText("_Create plan");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
    }

    private VBox createPlanCard(MembershipPlan plan) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        if (plan.archived()) {
            card.getStyleClass().add("plan-card-archived");
        }

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(plan.name());
        nameLabel.getStyleClass().add("heading");

        Label badgeLabel = new Label(plan.archived() ? "ARCHIVED" : "ACTIVE");
        badgeLabel.getStyleClass().add(plan.archived() ? "badge-archived" : "badge-active");

        header.getChildren().addAll(nameLabel, badgeLabel);

        Label detailsLabel = new Label(plan.durationDays() + " days · "
                + DisplayFormatters.price(plan.priceCents()));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (!plan.archived()) {
            Button editBtn = new Button("Edit");
            editBtn.setOnAction(_ -> startEdit(plan));
            actions.getChildren().add(editBtn);

            Button archiveBtn = new Button("Archive");
            archiveBtn.setOnAction(_ -> archivePlan(plan));
            actions.getChildren().add(archiveBtn);
        } else {
            Button restoreBtn = new Button("Restore");
            restoreBtn.setOnAction(_ -> restorePlan(plan));
            actions.getChildren().add(restoreBtn);
        }

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(_ -> deletePlan(plan));
        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, detailsLabel, actions);
        return card;
    }

    private void archivePlan(MembershipPlan plan) {
        status.info("Archiving plan…");
        Task<MembershipPlan> task = new Task<>() {
            @Override
            protected MembershipPlan call() throws Exception {
                return context.getMembershipPlanService().archive(plan.id());
            }
        };
        task.setOnSucceeded(_ -> refresh(() -> status.success("Plan '" + plan.name() + "' archived.")));
        task.setOnFailed(_ -> status.error(task.getException(), "Unable to archive plan."));
        Thread.ofPlatform().daemon().name("gymmie-archive-plan").start(task);
    }

    private void restorePlan(MembershipPlan plan) {
        status.info("Restoring plan…");
        Task<MembershipPlan> task = new Task<>() {
            @Override
            protected MembershipPlan call() throws Exception {
                return context.getMembershipPlanService().restore(plan.id());
            }
        };
        task.setOnSucceeded(_ -> refresh(() -> status.success("Plan '" + plan.name() + "' restored.")));
        task.setOnFailed(_ -> status.error(task.getException(), "Unable to restore plan."));
        Thread.ofPlatform().daemon().name("gymmie-restore-plan").start(task);
    }

    private void deletePlan(MembershipPlan plan) {
        status.info("Deleting plan…");
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return context.getMembershipPlanService().deleteOrArchive(plan.id());
            }
        };
        task.setOnSucceeded(_ -> {
            boolean deleted = task.getValue();
            refresh(() -> {
                if (deleted) {
                    status.success("Plan '" + plan.name() + "' deleted.");
                } else {
                    status.info("Plan '" + plan.name() + "' had purchase history and was archived instead.");
                }
            });
        });
        task.setOnFailed(_ -> status.error(task.getException(), "Unable to delete plan."));
        Thread.ofPlatform().daemon().name("gymmie-delete-plan").start(task);
    }

    private static int parsePriceCents(String input) throws ValidationException {
        if (input == null || input.isBlank()) {
            throw new ValidationException("Plan price must not be blank.");
        }
        String clean = input.strip().replace("SGD", "").replace("$", "").strip();
        try {
            if (clean.contains(".")) {
                BigDecimal decimal = new BigDecimal(clean);
                return decimal.movePointRight(2).intValueExact();
            }
            return Integer.parseInt(clean);
        } catch (Exception exception) {
            throw new ValidationException("Plan price must be a valid non-negative number.");
        }
    }
}
