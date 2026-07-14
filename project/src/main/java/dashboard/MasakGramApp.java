package dashboard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MasakGramApp extends Application {

    @Override
    public void start(Stage stage) {
        DashboardView dashboard = new DashboardView();

        Scene scene = new Scene(dashboard.getView(), 1200, 750);
        scene.getStylesheets().add(
                getClass().getResource("/style/dashboard.css").toExternalForm()
        );

        stage.setTitle("MasakGramPrompt Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}