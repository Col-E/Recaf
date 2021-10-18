package me.coley.recaf.ui.control.hex;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.WindowBase;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Launch extends Application {
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String path = "C:\\Code\\Java\\Obfuscator\\testing\\FxMain.class";
       // path = "C:\\Code\\Java\\Recaf3\\recaf-ui\\build\\classes\\" +
       //         "java\\main\\me\\coley\\recaf\\config\\Sample.class";
        byte[] clazz =  Files.readAllBytes(Paths.get(path));
        Lang.initialize();
        HexClassView hex = new HexClassView();
        hex.onUpdate(ClassInfo.read(clazz));
        primaryStage.setScene(new Scene(hex));
        primaryStage.setWidth(1100);
        primaryStage.setHeight(880);
        WindowBase.addStylesheets(primaryStage.getScene().getStylesheets());
        primaryStage.show();
    }
}