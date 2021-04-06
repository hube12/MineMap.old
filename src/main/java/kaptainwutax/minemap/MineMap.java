package kaptainwutax.minemap;

import com.formdev.flatlaf.*;
import kaptainwutax.minemap.feature.chests.Chests;
import kaptainwutax.minemap.init.*;
import kaptainwutax.minemap.ui.component.WorldTabs;
import kaptainwutax.minemap.ui.menubar.MenuBar;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class MineMap extends JFrame {

    public static MineMap INSTANCE;
    public static LookType lookType = LookType.DARCULA;
    public final static String ROOT_DIR = System.getProperty("user.home") + File.separatorChar + ".minemap";
    public final static String LOG_DIR = ROOT_DIR + File.separatorChar + "logs";
    public final static String SETTINGS_DIR = ROOT_DIR + File.separatorChar + "configs";
    public MenuBar toolbarPane;
    public WorldTabs worldTabs;

    public static void main(String[] args) throws IOException {
        createDirs();
        doRegister();
        INSTANCE = new MineMap();
        INSTANCE.setVisible(true);
        doDelayedRegister();
    }

    public static void createDirs() throws IOException {
        Path path;
        path = Paths.get(LOG_DIR);
        Files.createDirectories(path);
        path = Paths.get(SETTINGS_DIR);
        Files.createDirectories(path);
    }

    public static void doRegister() {
        Logger.registerLogger();
        Features.registerFeatures();
        Icons.registerIcons();
        Chests.registerChests();
        Configs.registerConfigs();
    }

    public static void doDelayedRegister() {
        // register keyboard event after the menus creation (very important)
        Configs.registerDelayedConfigs();
        KeyShortcuts.registerShortcuts();
        INSTANCE.doDelayedInitTasks();
    }

    public MineMap() {
        applyStyle();
        BorderLayout layout = new BorderLayout();
        this.setLayout(layout);
        this.initComponents();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setSize(screenSize.width / 2, screenSize.height / 2);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setTitle("MineMap");
        this.setIconImage(Icons.get(this.getClass()));
        System.out.println("Hello, its me");
        System.out.println("General Kenobi");
    }

    private void initComponents() {
        this.toolbarPane = new MenuBar();
        this.add(this.toolbarPane, BorderLayout.NORTH);

        this.worldTabs = new WorldTabs();
        this.add(this.worldTabs);
    }

    public void doDelayedInitTasks() {
        this.toolbarPane.doDelayedLabels();
    }

    public static void applyStyle() {
        try {
            lookType.setLookAndFeel();
        } catch (Exception e) {
            lookType = LookType.DARCULA;
            try {
                lookType.setLookAndFeel();
            } catch (Exception impossibleError) {
                impossibleError.printStackTrace();
            }
        }
    }

    public enum LookType {
        DARK("Dark", FlatDarkLaf::new),
        LIGHT("Light", FlatLightLaf::new),
        INTELLIJ("Intellij", FlatIntelliJLaf::new),
        DARCULA("Darcula", FlatDarculaLaf::new);

        private final String name;
        private final Supplier<FlatLaf> supplier;

        LookType(String name, Supplier<FlatLaf> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        public void setLookAndFeel() throws UnsupportedLookAndFeelException {
            UIManager.setLookAndFeel(supplier.get());
            for (Window window : JFrame.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        }

        public String getName() {
            return name;
        }

        public boolean isDark() {
            return supplier.get().isDark();
        }
    }

}
