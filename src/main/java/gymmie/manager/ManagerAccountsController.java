package gymmie.manager;

import java.io.IOException;
import java.util.List;

import gymmie.AppContext;
import gymmie.PasswordReveal;
import gymmie.Router;
import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.ui.StatusLabel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for the Manager account provisioning and lifecycle management
 * screen.
 *
 * <p>
 * Delegates all account operations to
 * {@link gymmie.manager.service.AccountProvisioningService}.
 * Supports creating Trainer and Member accounts, editing display names, and
 * deactivating/reactivating
 * accounts through mouse and keyboard interactions.
 */
public final class ManagerAccountsController {
    private final AppContext context;
    private final Router router;
    private Account editingAccount;

    @FXML
    private Button backButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Label formHeading;
    @FXML
    private TextField accountUsername;
    @FXML
    private Label passwordLabel;
    @FXML
    private HBox passwordBox;
    @FXML
    private PasswordField accountPassword;
    @FXML
    private Button accountPasswordReveal;
    @FXML
    private TextField accountDisplayName;
    @FXML
    private Label roleLabel;
    @FXML
    private ComboBox<Role> accountRole;
    @FXML
    private Button saveAccountButton;
    @FXML
    private Button cancelEditButton;
    @FXML
    private VBox accountList;
    @FXML
    private StatusLabel status;

    /**
     * Creates a controller using the application context and router.
     *
     * @param context application context containing services.
     * @param router  navigation coordinator.
     */
    public ManagerAccountsController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        PasswordReveal.install(accountPassword, accountPasswordReveal);
        accountRole.setItems(FXCollections.observableArrayList(Role.TRAINER, Role.MEMBER));
        accountRole.setValue(Role.TRAINER);
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
        status.info("Loading accounts…");
        Task<List<Account>> task = new Task<>() {
            @Override
            protected List<Account> call() throws Exception {
                return context.getAccountProvisioningService().getAllAccounts();
            }
        };
        task.setOnSucceeded(_ -> {
            refreshButton.setDisable(false);
            accountList.getChildren().clear();
            List<Account> accounts = task.getValue();
            for (Account account : accounts) {
                accountList.getChildren().add(createAccountCard(account));
            }
            if (onDone != null) {
                onDone.run();
            } else {
                status.info(accounts.isEmpty() ? "No accounts found." : "Accounts loaded.");
            }
        });
        task.setOnFailed(_ -> {
            refreshButton.setDisable(false);
            status.error(task.getException(), "Unable to load accounts. Please try Refresh again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-manager-accounts").start(task);
    }

    @FXML
    private void saveAccount() {
        if (saveAccountButton.isDisabled()) {
            return;
        }
        String displayName = accountDisplayName.getText();
        if (displayName == null || displayName.isBlank()) {
            status.error("Display name must not be blank.");
            accountDisplayName.requestFocus();
            return;
        }

        boolean isEdit = editingAccount != null;
        if (isEdit) {
            saveEdit(displayName.strip());
        } else {
            saveCreate(displayName.strip());
        }
    }

    private void saveEdit(String displayName) {
        saveAccountButton.setDisable(true);
        status.info("Updating account…");
        long targetId = editingAccount.id();

        Task<Account> task = new Task<>() {
            @Override
            protected Account call() throws Exception {
                return context.getAccountProvisioningService().edit(targetId, displayName);
            }
        };
        task.setOnSucceeded(_ -> {
            saveAccountButton.setDisable(false);
            resetForm();
            refresh(() -> status.success("Account updated."));
        });
        task.setOnFailed(_ -> {
            saveAccountButton.setDisable(false);
            status.error(task.getException(), "Unable to update account. Please check inputs and try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-save-account").start(task);
    }

    private void saveCreate(String displayName) {
        String username = accountUsername.getText();
        String password = accountPassword.getText();
        Role role = accountRole.getValue();

        if (username == null || username.isBlank()) {
            status.error("Username must not be blank.");
            accountUsername.requestFocus();
            return;
        }
        if (password == null || password.isBlank()) {
            status.error("Password must not be blank.");
            accountPassword.requestFocus();
            return;
        }
        if (role == null) {
            status.error("Please select a role.");
            accountRole.requestFocus();
            return;
        }

        saveAccountButton.setDisable(true);
        status.info("Provisioning account…");

        Task<Account> task = new Task<>() {
            @Override
            protected Account call() throws Exception {
                return context.getAccountProvisioningService().create(username.strip(), password, displayName, role);
            }
        };
        task.setOnSucceeded(_ -> {
            saveAccountButton.setDisable(false);
            resetForm();
            refresh(() -> status.success("Account provisioned."));
        });
        task.setOnFailed(_ -> {
            saveAccountButton.setDisable(false);
            status.error(task.getException(), "Unable to provision account. Please check inputs and try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-create-account").start(task);
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
     * Populates the account form to edit an existing account's display name.
     *
     * @param account existing account to edit.
     */
    public void startEdit(Account account) {
        editingAccount = account;
        formHeading.setText("Edit account: " + account.username());
        accountUsername.setText(account.username());
        accountUsername.setDisable(true);
        passwordLabel.setVisible(false);
        passwordLabel.setManaged(false);
        passwordBox.setVisible(false);
        passwordBox.setManaged(false);
        accountDisplayName.setText(account.displayName());
        roleLabel.setVisible(false);
        roleLabel.setManaged(false);
        accountRole.setVisible(false);
        accountRole.setManaged(false);
        saveAccountButton.setText("_Save changes");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        accountDisplayName.requestFocus();
    }

    private void resetForm() {
        editingAccount = null;
        formHeading.setText("Provision new account");
        accountUsername.clear();
        accountUsername.setDisable(false);
        passwordLabel.setVisible(true);
        passwordLabel.setManaged(true);
        accountPassword.clear();
        passwordBox.setVisible(true);
        passwordBox.setManaged(true);
        accountDisplayName.clear();
        roleLabel.setVisible(true);
        roleLabel.setManaged(true);
        accountRole.setVisible(true);
        accountRole.setManaged(true);
        accountRole.setValue(Role.TRAINER);
        saveAccountButton.setText("_Provision account");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
    }

    private VBox createAccountCard(Account account) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        if (!account.active()) {
            card.getStyleClass().add("account-card-deactivated");
        }

        FlowPane header = new FlowPane(12, 8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(account.displayName());
        nameLabel.setWrapText(true);
        nameLabel.maxWidthProperty().bind(card.widthProperty().subtract(48));
        nameLabel.getStyleClass().add("heading");

        Label usernameLabel = new Label("@" + account.username());

        usernameLabel.setWrapText(true);
        usernameLabel.maxWidthProperty().bind(card.widthProperty().subtract(48));

        Label roleBadge = new Label(account.role().name());
        roleBadge.getStyleClass().add("badge-role");

        Label statusBadge = new Label(account.active() ? "ACTIVE" : "DEACTIVATED");
        statusBadge.getStyleClass().add(account.active() ? "badge-active" : "badge-deactivated");

        header.getChildren().addAll(nameLabel, usernameLabel, roleBadge, statusBadge);

        FlowPane actions = actionsBox(account);
        card.getChildren().addAll(header, actions);
        return card;
    }

    private FlowPane actionsBox(Account account) {
        FlowPane actions = new FlowPane(10, 10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("Edit");
        editBtn.setOnAction(_ -> startEdit(account));
        actions.getChildren().add(editBtn);

        boolean isSeededManager = account.role() == Role.MANAGER
                && Account.SEEDED_MANAGER_USERNAME.equalsIgnoreCase(account.username());

        if (account.active()) {
            if (!isSeededManager) {
                Button deactivateBtn = new Button("Deactivate");
                deactivateBtn.setOnAction(_ -> deactivateAccount(account));
                actions.getChildren().add(deactivateBtn);
            }
        } else {
            Button reactivateBtn = new Button("Reactivate");
            reactivateBtn.setOnAction(_ -> reactivateAccount(account));
            actions.getChildren().add(reactivateBtn);
        }
        return actions;
    }

    private void deactivateAccount(Account account) {
        status.info("Deactivating account…");
        Task<Account> task = new Task<>() {
            @Override
            protected Account call() throws Exception {
                return context.getAccountProvisioningService().deactivate(account.id());
            }
        };
        task.setOnSucceeded(_ -> refresh(() -> status.success("Account '" + account.username() + "' deactivated.")));
        task.setOnFailed(_ -> status.error(task.getException(), "Unable to deactivate account."));
        Thread.ofPlatform().daemon().name("gymmie-deactivate-account").start(task);
    }

    private void reactivateAccount(Account account) {
        status.info("Reactivating account…");
        Task<Account> task = new Task<>() {
            @Override
            protected Account call() throws Exception {
                return context.getAccountProvisioningService().reactivate(account.id());
            }
        };
        task.setOnSucceeded(_ -> refresh(() -> status.success("Account '" + account.username() + "' reactivated.")));
        task.setOnFailed(_ -> status.error(task.getException(), "Unable to reactivate account."));
        Thread.ofPlatform().daemon().name("gymmie-reactivate-account").start(task);
    }
}
