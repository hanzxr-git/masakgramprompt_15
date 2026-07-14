package dashboard;

import java.util.ArrayList;
import java.util.List;
import Database.DashboardDAO;

import javafx.application.Platform;
import javafx.scene.control.TableRow;
import javafx.scene.layout.FlowPane;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.function.Supplier;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Main Dashboard View class for MasakGramPrompt application.
 * Handles all UI components, navigation, and user interactions.
 * This class manages the entire dashboard interface including:
 * - Navigation sidebar
 * - Dashboard statistics
 * - Experiment runner
 * - Reels management
 * - Nutrition facts comparison with hallucination detection
 * - Metrics and export functionality
 */
public class DashboardView {

    // ==================== UI COMPONENTS ====================
    
    private final BorderPane root = new BorderPane();          // Main layout container
    private ScrollPane mainScrollPane;                         // Scrollable content area
    private Label totalReelsValue;                             // Dashboard stat cards
    private Label totalTranscriptsValue;
    private Label totalExperimentsValue;
    private Label completedValue;
    private Label runningValue;
    private Label failedValue;
    private Label serverStatus;                                // System status indicator
    private TextArea activityLog;                              // TCP/IP activity log
    private TextArea reelPreview;                              // Reel preview text area
    private TextArea recentExperiments;                        // Recent experiments display
    private ProgressIndicator progressIndicator;               // Loading spinner
    private DashboardTCPClient activeClient;                   // TCP client for experiments
    private Button stopRunButton;                              // Stop analysis button
    
    /**
     * Constructor - Initializes the dashboard layout and shows the main page.
     */
    public DashboardView() {
        // Initialize main scroll pane
        mainScrollPane = new ScrollPane();
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Set sidebar and main content
        root.setLeft(createSidebar());
        root.setCenter(mainScrollPane);

        // Show default dashboard page
        showDashboardPage();
    }
    
    /**
     * Returns the main view for the dashboard.
     * @return Parent node containing the entire dashboard UI
     */
    public Parent getView() {
        return root;
    }

    // ==================== SIDEBAR CREATION ====================
    
    /**
     * Creates the navigation sidebar with menu buttons and system status.
     * @return VBox containing the sidebar components
     */
    private VBox createSidebar() {
        // Logo and title
        Label logo = new Label("🍳 MasakGramPrompt");
        logo.getStyleClass().add("logo");

        Label subtitle = new Label("Nutritional LLM Dashboard");
        subtitle.getStyleClass().add("sidebar-subtitle");

        // Navigation buttons
        Button dashboard = menuButton("🏠 Dashboard");
        Button reels = menuButton("🎬 Reels");
        Button experiments = menuButton("🧪 Experiments");
        Button results = menuButton("🥗 Nutrition Facts");
        Button metrics = menuButton("📈 Metrics");
        Button export = menuButton("📥 Export");
        
        // Set navigation actions - each button switches to its respective page
        dashboard.setOnAction(event -> showDashboardPage());
        reels.setOnAction(event -> showReelsPage());
        experiments.setOnAction(event -> showExperimentsPage());
        results.setOnAction(event -> showNutritionFactsPage());
        metrics.setOnAction(event -> showMetricsPage());
        export.setOnAction(event -> showExportPage());

        // System status section
        serverStatus = new Label("● TCP/IP Server: Checking...");
        serverStatus.getStyleClass().add("sidebar-subtitle");

        VBox systemBox = new VBox(8);
        systemBox.getStyleClass().add("system-box");
        systemBox.getChildren().addAll(
                new Label("System Status"),
                serverStatus,
                new Label("● MySQL: via Server"),
                new Label("● Ollama: via Server")
        );

        // Spacer to push system status to bottom
        Region spacer = new Region();

        // Main sidebar layout
        VBox sidebar = new VBox(18);
        sidebar.setPadding(new Insets(25));
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(
                logo,
                subtitle,
                dashboard,
                reels,
                experiments,
                results,
                metrics,
                export,
                spacer,
                systemBox
        );

        VBox.setVgrow(spacer, Priority.ALWAYS);

        return sidebar;
    }

    /**
     * Creates a styled menu button for the sidebar.
     * @param text Button label text
     * @return Styled Button
     */
    private Button menuButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    // ==================== DASHBOARD PAGE ====================
    
    /**
     * Creates the main dashboard content with stat cards and panels.
     * @return VBox containing all dashboard components
     */
    private VBox createMainContent() {
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");

        Label desc = new Label("Run LLM nutrition experiments for all transcripts using TCP/IP socket communication.");
        desc.getStyleClass().add("page-desc");

        // Stat cards in a flow layout
        FlowPane cards = new FlowPane();
        cards.setHgap(18);
        cards.setVgap(18);

        cards.getChildren().addAll(
                createStatCard("01", "Total Reels", "0", "Collected Instagram Reels", "reels"),
                createStatCard("02", "Transcripts", "0", "Verified transcripts", "transcripts"),
                createStatCard("03", "Experiments", "0", "Model × technique runs", "experiments"),
                createStatCard("04", "Completed", "0", "Completed analysis", "completed"),
                createStatCard("05", "Running", "0", "Currently processing", "running"),
                createStatCard("06", "Failed", "0", "Failed analysis", "failed")
        );

        // Middle row: Experiment runner + Live activity
        HBox middle = new HBox(18);
        middle.getChildren().addAll(createExperimentRunner(), createLivePanel());

        // Bottom row: Reel preview + Recent experiments
        HBox bottom = new HBox(18);
        bottom.getChildren().addAll(createReelPanel(), createRecentExperimentPanel());

        // Main content layout
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.getStyleClass().add("content");
        content.getChildren().addAll(title, desc, cards, middle, bottom);

        return content;
    }

    /**
     * Creates a statistics card for the dashboard.
     * Stores reference to value label for later updates.
     * @param icon Icon/emoji for the card
     * @param label Card label text
     * @param value Initial value
     * @param subtext Additional description
     * @param type Identifier for updating specific cards
     * @return VBox containing the stat card
     */
    private VBox createStatCard(String icon, String label, String value, String subtext, String type) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("card-icon");

        Label labelText = new Label(label);
        labelText.getStyleClass().add("card-label");

        Label valueText = new Label(value);
        valueText.getStyleClass().add("card-value");

        Label sub = new Label(subtext);
        sub.getStyleClass().add("card-subtext");

        // Store reference to value label for later updates when data loads
        if ("reels".equals(type)) {
            totalReelsValue = valueText;
        } else if ("transcripts".equals(type)) {
            totalTranscriptsValue = valueText;
        } else if ("experiments".equals(type)) {
            totalExperimentsValue = valueText;
        } else if ("completed".equals(type)) {
            completedValue = valueText;
        } else if ("running".equals(type)) {
            runningValue = valueText;
        } else if ("failed".equals(type)) {
            failedValue = valueText;
        }

        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(210);
        card.getChildren().addAll(iconLabel, labelText, valueText, sub);

        return card;
    }

    // ==================== EXPERIMENT RUNNER PANEL ====================
    
    /**
     * Creates the experiment runner panel with model selection, 
     * technique checkboxes, and run controls.
     * @return VBox containing the experiment runner UI
     */
    private VBox createExperimentRunner() {
        Label title = new Label("🧪 Experiment Runner");
        title.getStyleClass().add("section-title");

        Label instruction = new Label("Select one model and one or more prompt techniques.");
        instruction.getStyleClass().add("page-desc");

        // Model selection dropdown
        ComboBox<String> modelBox = new ComboBox<String>();
        modelBox.getItems().addAll(
                "llama3.2:3b",
                "phi4-mini",
                "qwen2.5:3b",
                "aisingapore/Gemma-SEA-LION-v4-4B-VL",
                "medgemma:4b"
        );
        modelBox.setPromptText("Select LLM Model");
        modelBox.setMaxWidth(Double.MAX_VALUE);

        // Prompt technique checkboxes
        CheckBox zeroShot = new CheckBox("zero-shot");
        CheckBox fewShot = new CheckBox("few-shot");
        CheckBox chainOfThought = new CheckBox("chain-of-thought");
        CheckBox structuredOutput = new CheckBox("structured-output");

        VBox techniqueBox = new VBox(8);
        techniqueBox.getChildren().addAll(zeroShot, fewShot, chainOfThought, structuredOutput);

        // Action buttons
        Button runButton = new Button("🚀 Run Analysis For All Transcripts");
        runButton.getStyleClass().add("primary-button");
        runButton.setMaxWidth(Double.MAX_VALUE);

        Button refreshButton = new Button("🔄 Refresh Dashboard");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        
        Button stopButton = new Button("⛔ Stop Analysis");
        stopButton.setDisable(true);
        stopRunButton = stopButton;
        stopButton.setOnAction(event -> stopRunningAnalysis());

        // Status indicators
        Label status = new Label("Status: Waiting");
        status.getStyleClass().add("status-text");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(45, 45);

        /**
         * Run button action - validates selections and starts experiment
         * Requires: 1 model selected, at least 1 technique selected
         */
        runButton.setOnAction(event -> {
            String selectedModel = modelBox.getValue();

            if (selectedModel == null) {
                status.setText("Status: Please select one LLM model.");
                return;
            }

            List<String> selectedTechniques = new ArrayList<String>();

            if (zeroShot.isSelected()) {
                selectedTechniques.add("zero-shot");
            }
            if (fewShot.isSelected()) {
                selectedTechniques.add("few-shot");
            }
            if (chainOfThought.isSelected()) {
                selectedTechniques.add("chain-of-thought");
            }
            if (structuredOutput.isSelected()) {
                selectedTechniques.add("structured-output");
            }

            if (selectedTechniques.isEmpty()) {
                status.setText("Status: Please select at least one prompt technique.");
                return;
            }

            runExperimentAsync(selectedModel, selectedTechniques);
        });

        refreshButton.setOnAction(event -> loadInitialData());

        // Main panel layout
        VBox box = new VBox(14);
        box.getStyleClass().add("panel");
        box.setPrefWidth(450);
        box.getChildren().addAll(
                title,
                instruction,
                modelBox,
                techniqueBox,
                runButton,
                stopButton,
                refreshButton,
                status,
                progressIndicator
        );

        return box;
    }

    /**
     * Creates the live TCP/IP activity log panel.
     * @return VBox containing the activity log
     */
    private VBox createLivePanel() {
        Label title = new Label("⚡ Live TCP/IP Activity");
        title.getStyleClass().add("section-title");

        activityLog = new TextArea();
        activityLog.setEditable(false);
        activityLog.setPrefHeight(300);
        activityLog.setText(
                "Ready.\n" +
                "Dashboard will send commands to TCP/IP Server at localhost:5000.\n\n" +
                "Expected command:\n" +
                "RUN_ALL_EXPERIMENTS|modelTag|technique1,technique2"
        );

        VBox box = new VBox(14);
        box.getStyleClass().add("panel");
        box.setPrefWidth(520);
        box.getChildren().addAll(title, activityLog);

        return box;
    }

    /**
     * Creates the reel preview panel showing recent reels.
     * @return VBox containing the reel preview
     */
    private VBox createReelPanel() {
        Label title = new Label("🎬 Reel Analysis Preview");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Should show reel ID, influencer handle, language tag, transcript status, and ground truth availability.");
        desc.getStyleClass().add("page-desc");

        reelPreview = new TextArea();
        reelPreview.setEditable(false);
        reelPreview.setPrefHeight(230);
        reelPreview.setText("Click Refresh Dashboard to load reel list from server.");

        VBox box = new VBox(14);
        box.getStyleClass().add("panel");
        box.setPrefWidth(480);
        box.getChildren().addAll(title, desc, reelPreview);

        return box;
    }

    /**
     * Creates the recent experiments panel.
     * @return VBox containing recent experiments
     */
    private VBox createRecentExperimentPanel() {
        Label title = new Label("📋 Recent Experiments");
        title.getStyleClass().add("section-title");

        Label desc = new Label("Shows model, technique, and pending/running/completed status.");
        desc.getStyleClass().add("page-desc");

        recentExperiments = new TextArea();
        recentExperiments.setEditable(false);
        recentExperiments.setPrefHeight(230);
        recentExperiments.setText("No experiment loaded yet.");

        VBox box = new VBox(14);
        box.getStyleClass().add("panel");
        box.setPrefWidth(480);
        box.getChildren().addAll(title, desc, recentExperiments);

        return box;
    }

    // ==================== EXPERIMENT EXECUTION ====================
    
    /**
     * Runs an experiment asynchronously using TCP/IP client.
     * Sends command to server to process all transcripts with selected model and techniques.
     * @param modelTag The LLM model to use
     * @param techniques List of prompt techniques to apply
     */
    private void runExperimentAsync(String modelTag, List<String> techniques) {
        // Show progress indicator and enable stop button
        progressIndicator.setVisible(true);
        if (stopRunButton != null) {
            stopRunButton.setDisable(false);
        }
        
        activityLog.setText(
                "Preparing experiment run...\n"
                        + "Model: " + modelTag + "\n"
                        + "Techniques: " + techniques + "\n\n"
        );

        // Background task to run experiments via TCP
        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                DashboardTCPClient client = new DashboardTCPClient();
                activeClient = client;

                return client.runAllExperiments(modelTag, techniques, message -> {
                    Platform.runLater(() -> {
                        activityLog.appendText(message + "\n");
                    });
                });
            }
        };

        // Handle successful completion
        task.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            activeClient = null;

            if (stopRunButton != null) {
                stopRunButton.setDisable(true);
            }
            activityLog.appendText("\nAnalysis process finished.\n");
            activityLog.appendText("Refreshing dashboard summary...\n");

            loadInitialData();
        });

        // Handle errors
        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);

            String message = task.getException() == null
                    ? "Unknown error"
                    : task.getException().getMessage();
            activeClient = null;

            if (stopRunButton != null) {
                stopRunButton.setDisable(true);
            }
            activityLog.appendText("\nERROR running analysis:\n" + message + "\n");

            loadInitialData();
        });

        // Start the background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Loads initial dashboard data from the database asynchronously.
     * Updates stat cards, reel preview, and recent experiments.
     */
    private void loadInitialData() {
        Task<String[]> task = new Task<String[]>() {
            @Override
            protected String[] call() {
                DashboardDAO dao = new DashboardDAO();

                String system = "OK";
                String summary = dao.getDashboardSummary();
                String reels = dao.getReelListPreview();
                String experiments = dao.getRecentExperiments();

                return new String[] { system, summary, reels, experiments };
            }
        };

        task.setOnSucceeded(event -> {
            String[] response = task.getValue();

            updateSystemStatus(response[0]);
            updateSummaryCards(response[1]);

            reelPreview.setText(response[2]);
            recentExperiments.setText(response[3]);

            activityLog.setText(
                    "Dashboard data loaded from MySQL.\n\n" +
                    response[1]
            );
        });

        task.setOnFailed(event -> {
            serverStatus.setText("● Database: Error");
            activityLog.setText("ERROR loading dashboard: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Updates the system status indicator.
     * @param response Status response from server
     */
    private void updateSystemStatus(String response) {
        if (response != null && response.startsWith("OK")) {
            serverStatus.setText("● TCP/IP Server: Connected");
        } else {
            serverStatus.setText("● TCP/IP Server: Disconnected");
        }
    }

    /**
     * Updates the dashboard summary stat cards with new values.
     * Parses the response from DAO and updates each card.
     * @param response Summary data from database
     */
    private void updateSummaryCards(String response) {
        if (response == null || response.startsWith("ERROR")) {
            activityLog.setText(response);
            return;
        }

        String[] lines = response.split("\\n");

        for (String line : lines) {
            if (line.startsWith("TOTAL_REELS=")) {
                totalReelsValue.setText(line.replace("TOTAL_REELS=", "").trim());
            } else if (line.startsWith("TOTAL_TRANSCRIPTS=")) {
                totalTranscriptsValue.setText(line.replace("TOTAL_TRANSCRIPTS=", "").trim());
            } else if (line.startsWith("TOTAL_EXPERIMENTS=")) {
                totalExperimentsValue.setText(line.replace("TOTAL_EXPERIMENTS=", "").trim());
            } else if (line.startsWith("COMPLETED=")) {
                completedValue.setText(line.replace("COMPLETED=", "").trim());
            } else if (line.startsWith("RUNNING=")) {
                if (runningValue != null) {
                    runningValue.setText(line.replace("RUNNING=", "").trim());
                }
            } else if (line.startsWith("FAILED=")) {
                if (failedValue != null) {
                    failedValue.setText(line.replace("FAILED=", "").trim());
                }
            }
        }
    }
    
    // ==================== PAGE NAVIGATION HELPERS ====================
    
    /**
     * Sets the current page content in the main scroll pane.
     * @param page The VBox page to display
     */
    private void setPage(VBox page) {
        mainScrollPane.setContent(page);
    }

    /**
     * Creates a base page layout with title and description.
     * @param titleText Page title
     * @param descriptionText Page description
     * @return VBox containing the base page layout
     */
    private VBox createPageBase(String titleText, String descriptionText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");

        Label desc = new Label(descriptionText);
        desc.getStyleClass().add("page-desc");

        VBox page = new VBox(20);
        page.setPadding(new Insets(25));
        page.getStyleClass().add("content");
        page.getChildren().addAll(title, desc);

        return page;
    }

    /**
     * Helper to create a large text area.
     * @param text Initial text content
     * @return Styled TextArea
     */
    private TextArea createLargeTextArea(String text) {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setPrefHeight(520);
        area.setText(text);
        return area;
    }

    /**
     * Helper to create a secondary style button.
     * @param text Button label
     * @return Styled Button
     */
    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    /**
     * Helper to load text asynchronously from a supplier.
     * @param area TextArea to update
     * @param supplier Function that provides the text
     */
    private void loadTextAsync(TextArea area, Supplier<String> supplier) {
        area.setText("Loading...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                return supplier.get();
            }
        };

        task.setOnSucceeded(event -> area.setText(task.getValue()));

        task.setOnFailed(event -> {
            String message = task.getException() == null
                    ? "Unknown error"
                    : task.getException().getMessage();

            area.setText("ERROR: " + message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ==================== PAGE DISPLAY METHODS ====================
    
    /**
     * Displays the main dashboard page.
     */
    private void showDashboardPage() {
        setPage(createMainContent());
        loadInitialData();
    }

    /**
     * Displays the Reels page with a table of all reels and transcript preview.
     * Features:
     * - Table showing all reels with their metadata
     * - Ability to view transcript preview for selected reel
     */
    private void showReelsPage() {
        VBox page = createPageBase(
                "Reels",
                "View collected Instagram Reels, transcript availability, ground truth status, and transcript preview."
        );

        TableView<String[]> table = new TableView<String[]>();

        // Define table columns
        table.getColumns().add(createColumn("Reel ID", 0, 80));
        table.getColumns().add(createColumn("Instagram ID", 1, 160));
        table.getColumns().add(createColumn("Influencer", 2, 150));
        table.getColumns().add(createColumn("Transcript", 3, 120));
        table.getColumns().add(createColumn("Ground Truth", 4, 130));
        table.getColumns().add(createColumn("Total Runs", 5, 100));
        table.getColumns().add(createColumn("Completed", 6, 100));
        table.getColumns().add(createColumn("Running", 7, 90));
        table.getColumns().add(createColumn("Failed", 8, 90));
        table.getColumns().add(createColumn("URL", 10, 420));
        table.setPrefHeight(390);

        TextArea previewArea = createLargeTextArea(
                "Select one reel row, then click View Transcript Preview."
        );
        previewArea.setPrefHeight(300);

        // Refresh button
        Button refreshButton = createSecondaryButton("🔄 Refresh Reels");
        refreshButton.setOnAction(event -> loadReelTableAsync(table));

        // Preview button - shows transcript for selected reel
        Button previewButton = createSecondaryButton("📄 View Transcript Preview");
        previewButton.setOnAction(event -> {
            String[] selectedRow = table.getSelectionModel().getSelectedItem();

            if (selectedRow == null || selectedRow.length == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Reel Selected");
                alert.setHeaderText("Please select a reel first.");
                alert.setContentText("Click one reel row, then click View Transcript Preview.");
                alert.showAndWait();
                return;
            }

            try {
                int reelId = Integer.parseInt(selectedRow[0]);
                loadTranscriptPreviewAsync(previewArea, reelId);

            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Reel");
                alert.setHeaderText("Cannot load transcript preview.");
                alert.setContentText("Selected row does not contain a valid reel ID.");
                alert.showAndWait();
            }
        });

        VBox tablePanel = new VBox(14);
        tablePanel.getStyleClass().add("panel");
        tablePanel.getChildren().addAll(refreshButton, previewButton, table);

        VBox previewPanel = new VBox(14);
        previewPanel.getStyleClass().add("panel");
        previewPanel.getChildren().addAll(new Label("Transcript Preview"), previewArea);

        page.getChildren().addAll(tablePanel, previewPanel);
        setPage(page);

        loadReelTableAsync(table);
    }

    /**
     * Displays the Experiments page with a table of all experiments.
     * Features:
     * - Table showing all experiments with their status
     * - Color coding for different statuses
     * - Ability to open nutrition fact sheet for selected experiment
     */
    
    private void showExperimentsPage() {

        VBox page = createPageBase(
                "Experiments",
                "Monitor experiment runs by model, prompt technique, and execution status."
        );

        DashboardDAO dao = new DashboardDAO();

        // =========================
        // FILTERS
        // =========================
        ComboBox<String> transcriptFilter = new ComboBox<>();
        ComboBox<String> modelFilter = new ComboBox<>();
        ComboBox<String> techniqueFilter = new ComboBox<>();

        transcriptFilter.getItems().addAll(dao.getTranscriptIds());
        modelFilter.getItems().addAll(dao.getModels());
        techniqueFilter.getItems().addAll(dao.getTechniques());

        transcriptFilter.setValue("Transcript");
        modelFilter.setValue("Model");
        techniqueFilter.setValue("Technique");

        transcriptFilter.setPromptText("Transcript ID");
        modelFilter.setPromptText("Model");
        techniqueFilter.setPromptText("Technique");

        Button filterButton = createSecondaryButton("Filter");

        HBox filterBar = new HBox(10);
        filterBar.getChildren().addAll(
                transcriptFilter,
                modelFilter,
                techniqueFilter,
                filterButton
        );

        // =========================
        // TABLE
        // =========================
        TableView<String[]> table = new TableView<>();

        applyExperimentStatusRowColor(table);

        table.getColumns().add(createColumn("Experiment ID", 0, 120));
        table.getColumns().add(createColumn("Transcript ID", 1, 120));

        TableColumn<String[], String> modelTechniqueColumn =
                new TableColumn<>("Model / Technique");

        modelTechniqueColumn.setCellValueFactory(data -> {

            String[] row = data.getValue();

            String model = row.length > 2 ? row[2] : "-";
            String technique = row.length > 3 ? row[3] : "-";

            return new SimpleStringProperty(model + " | " + technique);
        });

        modelTechniqueColumn.setPrefWidth(420);

        table.getColumns().add(modelTechniqueColumn);

        table.getColumns().add(createColumn("RAG", 4, 100));
        table.getColumns().add(createColumn("Status", 5, 120));
        table.getColumns().add(createColumn("Executed At", 6, 180));

        table.setPrefHeight(540);

        // =========================
        // BUTTONS
        // =========================
        Button refreshButton = createSecondaryButton("🔄 Refresh Experiments");

        refreshButton.setOnAction(event -> {

            transcriptFilter.setValue("All");
            modelFilter.setValue("All");
            techniqueFilter.setValue("All");

            loadExperimentTableAsync(table);

        });

        filterButton.setOnAction(event -> {

            table.setItems(
                    FXCollections.observableArrayList(
                            dao.getExperimentRowsFiltered(
                                    transcriptFilter.getValue(),
                                    modelFilter.getValue(),
                                    techniqueFilter.getValue()
                            )
                    )
            );

        });

        Button openFactSheetButton =
                createSecondaryButton("🥗 Open Nutrition Fact Sheet");

        openFactSheetButton.setOnAction(event -> {

            String[] selectedRow = table.getSelectionModel().getSelectedItem();

            if (selectedRow == null || selectedRow.length == 0) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Experiment Selected");
                alert.setHeaderText("Please select an experiment first.");
                alert.setContentText("Click one experiment row, then click Open Nutrition Fact Sheet.");
                alert.showAndWait();

                return;
            }

            try {

                int experimentId = Integer.parseInt(selectedRow[0]);

                showNutritionFactsPage(experimentId);

            } catch (NumberFormatException ex) {

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Experiment");
                alert.setHeaderText("Cannot open nutrition fact sheet.");
                alert.setContentText("Selected row does not contain a valid experiment ID.");
                alert.showAndWait();
            }

        });

        // =========================
        // PANEL
        // =========================
        VBox panel = new VBox(14);

        panel.getStyleClass().add("panel");

        panel.getChildren().addAll(
                filterBar,
                refreshButton,
                openFactSheetButton,
                table
        );

        page.getChildren().add(panel);

        setPage(page);

        loadExperimentTableAsync(table);
    }
    
    // ==================== NUTRITION FACTS PAGE ====================
    
    /**
     * Displays the Nutrition Facts page with GT and LLM tables side by side.
     * Shows hallucination detection in the LLM table.
     * This is the main page for comparing ground truth vs LLM extracted ingredients.
     */
    private void showNutritionFactsPage() {
        showNutritionFactsPage(null);
    }

    /**
     * Displays the Nutrition Facts page for a specific experiment.
     * Features:
     * - GT table showing ground truth ingredients (human-annotated)
     * - LLM table showing extracted ingredients with hallucination detection
     * - Hallucination statistics in the overview
     * - Side-by-side comparison layout
     * 
     * @param experimentId Optional experiment ID, null for latest
     */
    private void showNutritionFactsPage(Integer experimentId) {
        String description;

        if (experimentId == null) {
            description = "Compare human-annotated ground truth ingredients with the latest LLM-extracted ingredients.";
        } else {
            description = "Compare human-annotated ground truth ingredients with LLM output for Experiment ID: " + experimentId;
        }

        VBox page = createPageBase(
                "Nutrition Facts",
                description
        );

        // Overview area with statistics
        TextArea overviewArea = createLargeTextArea("Loading nutrition comparison...");
        overviewArea.setPrefHeight(260);

        // ===== GROUND TRUTH TABLE =====
        // Shows only GT columns (indices 1-6 from the full row)
        TableView<String[]> gtTable = new TableView<String[]>();
        gtTable.getColumns().add(createColumn("#", 0, 50));
        gtTable.getColumns().add(createColumn("GT Name Original", 1, 160));
        gtTable.getColumns().add(createColumn("GT Name English", 2, 150));
        gtTable.getColumns().add(createColumn("GT Quantity Expression", 3, 190));
        gtTable.getColumns().add(createColumn("GT Value / Unit", 4, 140));
        gtTable.getColumns().add(createColumn("GT Weight (g)", 5, 120));
        gtTable.getColumns().add(createColumn("GT Calories", 6, 110));
        gtTable.setPrefHeight(450);

        // ===== LLM TABLE =====
        // Shows LLM columns with hallucination detection column added
        TableView<String[]> llmTable = new TableView<String[]>();
        llmTable.getColumns().add(createColumn("#", 0, 50));
        llmTable.getColumns().add(createColumn("LLM Name Original", 1, 170));
        llmTable.getColumns().add(createColumn("LLM Name English", 2, 160));
        llmTable.getColumns().add(createColumn("LLM Quantity", 3, 120));
        llmTable.getColumns().add(createColumn("LLM Unit", 4, 160));
        llmTable.getColumns().add(createColumn("LLM Weight (g)", 5, 120));
        llmTable.getColumns().add(createColumn("LLM Calories", 6, 120));
        
        // Hallucination status column - shows if LLM ingredient is hallucinated
        TableColumn<String[], String> hallucinationColumn = new TableColumn<String[], String>("Hallucinated?");
        hallucinationColumn.setCellValueFactory(data -> {
            String[] row = data.getValue();

            if (row != null && row.length > 7) {
                return new SimpleStringProperty(row[7]);
            }

            return new SimpleStringProperty("-");
        });
        hallucinationColumn.setPrefWidth(120);
        llmTable.getColumns().add(hallucinationColumn);
        llmTable.setPrefHeight(450);

        // Refresh button
        Button refreshButton = createSecondaryButton("🔄 Refresh Nutrition Comparison");
        refreshButton.setOnAction(event -> {
            loadNutritionComparisonAsync(gtTable, llmTable, overviewArea, experimentId);
        });

        // Download CSV button
        Button downloadButton = createSecondaryButton("📥 Download This Fact Sheet CSV");
        downloadButton.setOnAction(event -> {
            exportNutritionFactSheetAsync(experimentId, overviewArea);
        });

        // GT panel with label
        VBox gtPanel = new VBox(10);
        gtPanel.getStyleClass().add("panel");
        Label gtLabel = new Label("🔍 Ground Truth Ingredients (Human-Annotated)");
        gtLabel.getStyleClass().add("section-title");
        gtPanel.getChildren().addAll(gtLabel, gtTable);

        // LLM panel with label
        VBox llmPanel = new VBox(10);
        llmPanel.getStyleClass().add("panel");
        Label llmLabel = new Label("🤖 LLM Extracted Ingredients");
        llmLabel.getStyleClass().add("section-title");
        llmPanel.getChildren().addAll(llmLabel, llmTable);

        // Side-by-side layout for GT and LLM tables
        HBox tablesContainer = new HBox(20);
        tablesContainer.getChildren().addAll(gtPanel, llmPanel);
        HBox.setHgrow(gtPanel, Priority.ALWAYS);
        HBox.setHgrow(llmPanel, Priority.ALWAYS);

        // Overview panel with buttons
        VBox overviewPanel = new VBox(14);
        overviewPanel.getStyleClass().add("panel");
        overviewPanel.getChildren().addAll(refreshButton, downloadButton, overviewArea);

        page.getChildren().addAll(overviewPanel, tablesContainer);
        setPage(page);

        loadNutritionComparisonAsync(gtTable, llmTable, overviewArea, experimentId);
    }

    /**
     * Loads nutrition comparison data asynchronously.
     * This is the CRITICAL method that:
     * 1. Loads data from database
     * 2. Splits into GT and LLM tables
     * 3. Performs hallucination detection in-memory
     * 4. Displays hallucination status in LLM table
     * 
     * @param gtTable TableView for ground truth data
     * @param llmTable TableView for LLM data with hallucination status
     * @param overviewArea TextArea for overview statistics
     * @param experimentId Optional experiment ID
     */
    private void loadNutritionComparisonAsync(TableView<String[]> gtTable, TableView<String[]> llmTable, 
                                            TextArea overviewArea, Integer experimentId) {
        // Load the full data once and split into GT and LLM tables
        Task<java.util.List<String[]>> task = new Task<java.util.List<String[]>>() {
            @Override
            protected java.util.List<String[]> call() {
                DashboardDAO dao = new DashboardDAO();

                if (experimentId == null) {
                    return dao.getLatestNutritionComparisonRows();
                }

                return dao.getNutritionComparisonRowsByExperiment(experimentId);
            }
        };

        task.setOnSucceeded(event -> {
            java.util.List<String[]> fullRows = task.getValue();
            
            // ===== HALLUCINATION DETECTION =====
            // Collect all GT ingredient names for comparison
         // =========================
         // Build GT ingredient set
         // =========================

         java.util.Set<String> gtIngredients = new java.util.HashSet<String>();

         for (String[] row : fullRows) {

             String gtOriginal = row.length > 1 ? row[1] : "";
             String gtEnglish  = row.length > 2 ? row[2] : "";

             if (gtOriginal != null &&
                 !gtOriginal.trim().isEmpty() &&
                 !gtOriginal.equals("-")) {

                 gtIngredients.add(gtOriginal.trim().toLowerCase());
             }

             if (gtEnglish != null &&
                 !gtEnglish.trim().isEmpty() &&
                 !gtEnglish.equals("-")) {

                 gtIngredients.add(gtEnglish.trim().toLowerCase());
             }
         }

         // =========================
         // GT table
         // =========================

         java.util.List<String[]> gtRows = new java.util.ArrayList<>();

         for (String[] row : fullRows) {

             String[] gtRow = new String[7];

             for (int i = 0; i <= 6 && i < row.length; i++) {
                 gtRow[i] = row[i];
             }

             gtRows.add(gtRow);
         }

         // =========================
         // LLM table
         // =========================

         java.util.List<String[]> llmRows = new java.util.ArrayList<>();

         java.util.Set<String> matchedGT = new java.util.HashSet<>();

         for (String[] row : fullRows) {

             String original = row.length > 7 ? row[7] : "";
             String english  = row.length > 8 ? row[8] : "";

             if ((original == null || original.trim().isEmpty() || original.equals("-")) &&
                 (english == null || english.trim().isEmpty() || english.equals("-"))) {
                 continue;
             }

             String[] llmRow = new String[8];

             llmRow[0] = String.valueOf(llmRows.size() + 1);
             llmRow[1] = row.length > 7 ? row[7] : "-";
             llmRow[2] = row.length > 8 ? row[8] : "-";
             llmRow[3] = row.length > 9 ? row[9] : "-";
             llmRow[4] = row.length > 10 ? row[10] : "-";
             llmRow[5] = row.length > 11 ? row[11] : "-";
             llmRow[6] = row.length > 12 ? row[12] : "-";

             boolean valid = false;

             String originalLower = original == null ? "" : original.toLowerCase().trim();
             String englishLower = english == null ? "" : english.toLowerCase().trim();

             for (String gt : gtIngredients) {

                 if (isIngredientMatch(originalLower, gt) ||
                     isIngredientMatch(englishLower, gt)) {

                     valid = true;
                     matchedGT.add(gt);
                     break;
                 }
             }

             llmRow[7] = valid
                     ? "✅ Valid"
                     : "❌ Hallucinated";

             llmRows.add(llmRow);
         }

         // =========================
         // Missing ingredients
         // =========================

         for (String gt : gtIngredients) {

             if (!matchedGT.contains(gt)) {

                 String[] missing = new String[8];

                 missing[0] = String.valueOf(llmRows.size() + 1);
                 missing[1] = "-";
                 missing[2] = "-";
                 missing[3] = "-";
                 missing[4] = "-";
                 missing[5] = "-";
                 missing[6] = "-";
                 missing[7] = "⚠ Missing";

                 llmRows.add(missing);
             }
         }
            
            gtTable.setItems(FXCollections.observableArrayList(gtRows));
            llmTable.setItems(FXCollections.observableArrayList(llmRows));
            
            // ===== UPDATE OVERVIEW WITH HALLUCINATION STATS =====
            long totalLLM = llmRows.size();
            long hallucinatedCount = llmRows.stream()
                    .filter(r -> r[7].contains("Hallucinated"))
                    .count();

            long missingCount = llmRows.stream()
                    .filter(r -> r[7].contains("Missing"))
                    .count();

            long validCount = llmRows.stream()
                    .filter(r -> r[7].contains("Valid"))
                    .count();
            
            String existingOverview = overviewArea.getText();
            overviewArea.setText(
                    existingOverview +
                    "\n==================================================\n" +
                    "🔍 HALLUCINATION ANALYSIS\n" +
                    "==================================================\n" +
                    "Valid Ingredients: " + validCount + "\n" +
                    "Hallucinated: " + hallucinatedCount + "\n" +
                    "Missing: " + missingCount + "\n\n" +
                    "✅ Valid = Exists in Ground Truth\n" +
                    "❌ Hallucinated = Predicted but not in Ground Truth\n" +
                    "⚠ Missing = Exists in Ground Truth but not predicted by LLM"
            );
        });

        task.setOnFailed(event -> {
            java.util.List<String[]> errorRows = new java.util.ArrayList<String[]>();
            String message = task.getException() == null ? "Unknown error" : task.getException().getMessage();
            errorRows.add(new String[] {"ERROR", message, "-", "-", "-", "-", "-"});
            gtTable.setItems(FXCollections.observableArrayList(errorRows));
            llmTable.setItems(FXCollections.observableArrayList(errorRows));
        });

        // Load overview from DAO
        Task<String> overviewTask = new Task<String>() {
            @Override
            protected String call() {
                DashboardDAO dao = new DashboardDAO();

                if (experimentId == null) {
                    return dao.getLatestNutritionComparisonOverview();
                }

                return dao.getNutritionComparisonOverviewByExperiment(experimentId);
            }
        };

        overviewTask.setOnSucceeded(event -> {
            // Append DAO overview to existing hallucination stats
            String daoOverview = overviewTask.getValue();
            String currentText = overviewArea.getText();
            // Only show DAO overview if hallucination stats already shown
            if (currentText.contains("HALLUCINATION ANALYSIS")) {
                // Keep the hallucination stats and add DAO overview below
                overviewArea.setText(currentText + "\n\n" + daoOverview);
            } else {
                overviewArea.setText(daoOverview);
            }
        });

        overviewTask.setOnFailed(event -> {
            String message = overviewTask.getException() == null
                    ? "Unknown error"
                    : overviewTask.getException().getMessage();

            overviewArea.appendText("\n\nERROR loading overview: " + message);
        });

        // Start threads
        Thread tableThread = new Thread(task);
        tableThread.setDaemon(true);
        tableThread.start();

        Thread overviewThread = new Thread(overviewTask);
        overviewThread.setDaemon(true);
        overviewThread.start();
    }

    /**
     * Helper method to check if two ingredients match.
     * Uses multiple matching strategies:
     * 1. Exact match
     * 2. Contains match
     * 3. Common word removal match
     * 
     * @param llmName LLM extracted ingredient name
     * @param gtName Ground truth ingredient name
     * @return true if ingredients match, false otherwise
     */
    private boolean isIngredientMatch(String llmName, String gtName) {
        if (llmName == null || gtName == null) {
            return false;
        }
        
        // Exact match
        if (llmName.equals(gtName)) {
            return true;
        }
        
        // Contains match (e.g., "garlic powder" contains "garlic")
        if (llmName.contains(gtName) || gtName.contains(llmName)) {
            return true;
        }
        
        // Remove common words and check (e.g., "fresh garlic" vs "garlic")
        String[] commonWords = {"fresh", "dried", "ground", "whole", "chopped", "minced", "crushed", "organic"};
        String llmCleaned = llmName;
        String gtCleaned = gtName;
        
        for (String word : commonWords) {
            llmCleaned = llmCleaned.replace(word, "").trim();
            gtCleaned = gtCleaned.replace(word, "").trim();
        }
        
        if (llmCleaned.equals(gtCleaned) || llmCleaned.contains(gtCleaned) || gtCleaned.contains(llmCleaned)) {
            return true;
        }
        
        return false;
    }
    
    // ==================== METRICS PAGE ====================
    
    /**
     * Displays the Metrics page with performance metrics for all models and techniques.
     * Features:
     * - Overview statistics
     * - Table showing metrics by model and technique
     * - JSON validity, hallucination rates
     * - Evaluation layer guide
     */
    private void showMetricsPage() {
        VBox page = createPageBase(
                "📊 Performance Metrics",
                "Evaluate LLM performance across different models and prompt techniques. " +
                "Track JSON validity, hallucination rates, and experiment success metrics " +
                "to identify the best-performing configurations."
        );

        TextArea overviewArea = createLargeTextArea("Loading metrics overview...");
        overviewArea.setPrefHeight(260);

        TableView<String[]> table = new TableView<String[]>();

        table.getColumns().add(createColumn("Model", 0, 260));
        table.getColumns().add(createColumn("Technique", 1, 170));
        table.getColumns().add(createColumn("Total Runs", 2, 100));
        table.getColumns().add(createColumn("Completed", 3, 110));
        table.getColumns().add(createColumn("Failed", 4, 90));
        table.getColumns().add(createColumn("Running", 5, 90));
        table.getColumns().add(createColumn("JSON Validity", 6, 130));
        table.getColumns().add(createColumn("LLM Ingredients", 7, 110));
        table.getColumns().add(createColumn("Hallucinated", 8, 120));
        table.getColumns().add(createColumn("Hallucination Rate", 9, 160));

        table.setPrefHeight(420);

        Button refreshButton = createSecondaryButton("🔄 Refresh Metrics");
        refreshButton.setOnAction(event -> loadMetricsTableAsync(table, overviewArea));

        VBox overviewPanel = new VBox(14);
        overviewPanel.getStyleClass().add("panel");
        overviewPanel.getChildren().addAll(refreshButton, overviewArea);

        VBox tablePanel = new VBox(14);
        tablePanel.getStyleClass().add("panel");
        
        // Add a small description above the table
        Label tableDescription = new Label(
            "📋 Model Performance Summary: " +
            "Shows how each model-technique combination performs across key metrics."
        );
        tableDescription.getStyleClass().add("page-desc");
        tableDescription.setWrapText(true);
        
        tablePanel.getChildren().addAll(tableDescription, table);
        
        // ===== EVALUATION LAYER GUIDE =====
        TableView<String[]> guideTable = new TableView<String[]>();

        guideTable.getColumns().add(createColumn("Layer", 0, 100));
        guideTable.getColumns().add(createColumn("CSV File", 1, 230));
        guideTable.getColumns().add(createColumn("Purpose", 2, 420));
        guideTable.getColumns().add(createColumn("Metric Type", 3, 180));

        java.util.List<String[]> guideRows = new java.util.ArrayList<String[]>();

        guideRows.add(new String[] {
                "Layer 1A",
                "layer1a_exact_match.csv",
                "Compares predicted ingredient/unit text with ground truth exactly.",
                "Text Accuracy"
        });

        guideRows.add(new String[] {
                "Layer 1B",
                "layer1b_text_similarity.csv",
                "Measures similarity between predicted and ground truth ingredient names.",
                "Text Similarity"
        });

        guideRows.add(new String[] {
                "Layer 2A",
                "layer2a_numeric_quantity.csv",
                "Compares extracted quantity values and estimated ingredient weights.",
                "Numeric Accuracy"
        });

        guideRows.add(new String[] {
                "Layer 2B",
                "layer2b_numeric_nutrition.csv",
                "Compares per-ingredient nutrition values such as calories, protein, fat, and carbs.",
                "Nutrition Accuracy"
        });

        guideRows.add(new String[] {
                "Layer 2C",
                "layer2c_nutrition_totals.csv",
                "Compares full recipe-level nutrition totals.",
                "Recipe Total Accuracy"
        });

        guideRows.add(new String[] {
                "Layer 3A",
                "layer3a_json_validity.csv",
                "Checks whether LLM output is valid and parseable JSON.",
                "Output Quality"
        });

        guideRows.add(new String[] {
                "Layer 3B",
                "layer3b_hallucination.csv",
                "Checks whether extracted ingredients are hallucinated or unsupported.",
                "Hallucination"
        });

        guideRows.add(new String[] {
                "Layer 3C",
                "layer3c_ingredient_detection.csv",
                "Measures ingredient detection performance using precision, recall, and F1.",
                "Detection"
        });

        guideRows.add(new String[] {
                "Layer 4",
                "layer4_human_evaluation.csv",
                "Stores human evaluation scores such as fluency, completeness, and plausibility.",
                "Human Rating"
        });

        guideRows.add(new String[] {
                "Layer 5",
                "layer5_condition_scores.csv",
                "Prepares model and prompt condition scores for statistical comparison.",
                "Statistical Test"
        });

        guideTable.setItems(FXCollections.observableArrayList(guideRows));
        guideTable.setPrefHeight(330);

        VBox guidePanel = new VBox(14);
        guidePanel.getStyleClass().add("panel");
        
        Label guideTitle = new Label("📚 Evaluation Layer Guide");
        guideTitle.getStyleClass().add("section-title");
        
        Label guideDescription = new Label(
            "These evaluation layers measure different aspects of LLM performance, " +
            "from text accuracy to nutrition value correctness. Each layer focuses " +
            "on a specific evaluation dimension."
        );
        guideDescription.getStyleClass().add("page-desc");
        guideDescription.setWrapText(true);
        
        guidePanel.getChildren().addAll(guideTitle, guideDescription, guideTable);
        
        page.getChildren().addAll(overviewPanel, tablePanel, guidePanel);
        setPage(page);

        loadMetricsTableAsync(table, overviewArea);
    }

    // ==================== EXPORT PAGE ====================
    
    /**
     * Displays the Export page with buttons for each CSV export type.
     * Exports are generated asynchronously and saved to the exports folder.
     */
    private void showExportPage() {
        VBox page = createPageBase(
                "Export",
                "Download CSV files for evaluation and reporting."
        );

        Label status = new Label("Choose one CSV export option.");
        status.getStyleClass().add("status-text");

        // Create export buttons for each evaluation layer
        Button exactMatch = createSecondaryButton("Download Exact Match CSV");
        Button textSimilarity = createSecondaryButton("Download Text Similarity CSV");
        Button numericQuantity = createSecondaryButton("Download Numeric Quantity CSV");
        Button numericNutrition = createSecondaryButton("Download Numeric Nutrition CSV");
        Button nutritionTotals = createSecondaryButton("Download Nutrition Totals CSV");
        Button jsonValidity = createSecondaryButton("Download JSON Validity CSV");
        Button hallucination = createSecondaryButton("Download Hallucination CSV");
        Button ingredientDetection = createSecondaryButton("Download Ingredient Detection CSV");
        Button humanEvaluation = createSecondaryButton("Download Human Evaluation CSV");
        Button conditionScores = createSecondaryButton("Download Condition Scores CSV");

        // Set export actions
        exactMatch.setOnAction(event -> exportCsvAsync("exact_match", status));
        textSimilarity.setOnAction(event -> exportCsvAsync("text_similarity", status));
        numericQuantity.setOnAction(event -> exportCsvAsync("numeric_quantity", status));
        numericNutrition.setOnAction(event -> exportCsvAsync("numeric_nutrition", status));
        nutritionTotals.setOnAction(event -> exportCsvAsync("nutrition_totals", status));
        jsonValidity.setOnAction(event -> exportCsvAsync("json_validity", status));
        hallucination.setOnAction(event -> exportCsvAsync("hallucination", status));
        ingredientDetection.setOnAction(event -> exportCsvAsync("ingredient_detection", status));
        humanEvaluation.setOnAction(event -> exportCsvAsync("human_evaluation", status));
        conditionScores.setOnAction(event -> exportCsvAsync("condition_scores", status));

        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel");
        panel.getChildren().addAll(
                status,
                exactMatch,
                textSimilarity,
                numericQuantity,
                numericNutrition,
                nutritionTotals,
                jsonValidity,
                hallucination,
                ingredientDetection,
                humanEvaluation,
                conditionScores
        );

        page.getChildren().add(panel);
        setPage(page);
    }

    /**
     * Exports a CSV file asynchronously.
     * @param exportType Type of export to generate
     * @param status Label to update with status messages
     */
    private void exportCsvAsync(String exportType, Label status) {
        status.setText("Exporting " + exportType + "...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.exportCsv(exportType);
            }
        };

        task.setOnSucceeded(event -> status.setText(task.getValue()));

        task.setOnFailed(event -> {
            String message = task.getException() == null
                    ? "Unknown error"
                    : task.getException().getMessage();

            status.setText("ERROR: " + message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    // ==================== TABLE HELPERS ====================
    
    /**
     * Creates a table column with the given title, index, and width.
     * Used for all TableView columns in the dashboard.
     * @param title Column header text
     * @param index Index in the String array
     * @param width Column width in pixels
     * @return Configured TableColumn
     */
    private TableColumn<String[], String> createColumn(String title, int index, double width) {
        TableColumn<String[], String> column = new TableColumn<String[], String>(title);

        column.setCellValueFactory(data -> {
            String[] row = data.getValue();

            if (row == null || index >= row.length) {
                return new SimpleStringProperty("-");
            }

            return new SimpleStringProperty(row[index]);
        });

        column.setPrefWidth(width);
        return column;
    }

    /**
     * Loads reel table data asynchronously from the database.
     * @param table TableView to populate
     */
    private void loadReelTableAsync(TableView<String[]> table) {
        Task<java.util.List<String[]>> task = new Task<java.util.List<String[]>>() {
            @Override
            protected java.util.List<String[]> call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.getReelRows();
            }
        };

        task.setOnSucceeded(event -> {
            table.setItems(FXCollections.observableArrayList(task.getValue()));
        });

        task.setOnFailed(event -> {
            java.util.List<String[]> errorRows = new java.util.ArrayList<String[]>();

            errorRows.add(new String[] {
                    "ERROR",
                    "-",
                    task.getException().getMessage(),
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-"
            });

            table.setItems(FXCollections.observableArrayList(errorRows));
        });
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Loads experiment table data asynchronously from the database.
     * @param table TableView to populate
     */
    private void loadExperimentTableAsync(TableView<String[]> table) {
        Task<java.util.List<String[]>> task = new Task<java.util.List<String[]>>() {
            @Override
            protected java.util.List<String[]> call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.getExperimentRows();
            }
        };

        task.setOnSucceeded(event -> {
            table.setItems(FXCollections.observableArrayList(task.getValue()));
        });

        task.setOnFailed(event -> {
            java.util.List<String[]> errorRows = new java.util.ArrayList<String[]>();

            errorRows.add(new String[] {
                    "ERROR",
                    "-",
                    task.getException().getMessage(),
                    "-",
                    "-",
                    "-",
                    "-"
            });

            table.setItems(FXCollections.observableArrayList(errorRows));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Applies color coding to experiment status rows.
     * Colors: Completed (green), Running (blue), Failed (red), Pending (yellow)
     * @param table TableView to apply styling to
     */
    private void applyExperimentStatusRowColor(TableView<String[]> table) {
        table.setRowFactory(tv -> new TableRow<String[]>() {
            @Override
            protected void updateItem(String[] row, boolean empty) {
                super.updateItem(row, empty);

                getStyleClass().removeAll(
                        "row-completed",
                        "row-running",
                        "row-failed",
                        "row-pending"
                );

                if (empty || row == null || row.length < 6) {
                    return;
                }

                String status = row[5];

                if (status == null) {
                    return;
                }

                status = status.toLowerCase();

                if ("completed".equals(status)) {
                    getStyleClass().add("row-completed");
                } else if ("running".equals(status)) {
                    getStyleClass().add("row-running");
                } else if ("failed".equals(status)) {
                    getStyleClass().add("row-failed");
                } else if ("pending".equals(status)) {
                    getStyleClass().add("row-pending");
                }
            }
        });
    }
    
    /**
     * Loads metrics table data and overview asynchronously.
     * @param table TableView to populate with metrics
     * @param overviewArea TextArea to populate with overview
     */
    private void loadMetricsTableAsync(TableView<String[]> table, TextArea overviewArea) {
        Task<java.util.List<String[]>> tableTask = new Task<java.util.List<String[]>>() {
            @Override
            protected java.util.List<String[]> call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.getMetricsRows();
            }
        };

        tableTask.setOnSucceeded(event -> {
            table.setItems(FXCollections.observableArrayList(tableTask.getValue()));
        });

        tableTask.setOnFailed(event -> {
            java.util.List<String[]> errorRows = new java.util.ArrayList<String[]>();

            errorRows.add(new String[] {
                    "ERROR",
                    tableTask.getException().getMessage(),
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-"
            });

            table.setItems(FXCollections.observableArrayList(errorRows));
        });

        Task<String> overviewTask = new Task<String>() {
            @Override
            protected String call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.getMetricsOverview();
            }
        };

        overviewTask.setOnSucceeded(event -> overviewArea.setText(overviewTask.getValue()));

        overviewTask.setOnFailed(event -> {
            String message = overviewTask.getException() == null
                    ? "Unknown error"
                    : overviewTask.getException().getMessage();

            overviewArea.setText("ERROR: " + message);
        });

        Thread tableThread = new Thread(tableTask);
        tableThread.setDaemon(true);
        tableThread.start();

        Thread overviewThread = new Thread(overviewTask);
        overviewThread.setDaemon(true);
        overviewThread.start();
    }
    
    /**
     * Loads transcript preview for a specific reel ID asynchronously.
     * @param previewArea TextArea to display the transcript
     * @param reelId The reel ID to load transcript for
     */
    private void loadTranscriptPreviewAsync(TextArea previewArea, int reelId) {
        previewArea.setText("Loading transcript preview for Reel ID: " + reelId + "...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.getTranscriptPreviewByReelId(reelId);
            }
        };

        task.setOnSucceeded(event -> previewArea.setText(task.getValue()));

        task.setOnFailed(event -> {
            String message = task.getException() == null
                    ? "Unknown error"
                    : task.getException().getMessage();

            previewArea.setText("ERROR: " + message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Exports nutrition fact sheet as CSV asynchronously.
     * @param experimentId Experiment ID to export (null for latest)
     * @param overviewArea TextArea to show export status
     */
    private void exportNutritionFactSheetAsync(Integer experimentId, TextArea overviewArea) {
        overviewArea.appendText("\n\nExporting nutrition fact sheet CSV...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.exportNutritionFactSheetCsv(experimentId);
            }
        };

        task.setOnSucceeded(event -> {
            overviewArea.appendText("\n\n" + task.getValue());
        });

        task.setOnFailed(event -> {
            String message = task.getException() == null
                    ? "Unknown error"
                    : task.getException().getMessage();

            overviewArea.appendText("\n\nERROR exporting fact sheet CSV: " + message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    // ==================== STOP ANALYSIS ====================
    
    /**
     * Stops the currently running analysis.
     * Marks all running experiments as failed in the database.
     */
    private void stopRunningAnalysis() {
        activityLog.appendText("\nStop requested by user...\n");

        if (stopRunButton != null) {
            stopRunButton.setDisable(true);
        }

        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }

        if (activeClient != null) {
            activeClient.stopCurrentRun();
        }

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                DashboardDAO dao = new DashboardDAO();
                return dao.markRunningExperimentsAsFailed();
            }
        };

        task.setOnSucceeded(event -> {
            activityLog.appendText(task.getValue() + "\n");
            activityLog.appendText("Dashboard refreshed after stop.\n");
            loadInitialData();
        });

        task.setOnFailed(event -> {
            String message = task.getException() == null
                    ? "Unknown error"
                    : task.getException().getMessage();

            activityLog.appendText("ERROR stopping analysis: " + message + "\n");
            loadInitialData();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}