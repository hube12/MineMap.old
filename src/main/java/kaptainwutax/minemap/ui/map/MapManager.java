package kaptainwutax.minemap.ui.map;

import kaptainwutax.mathutils.util.Mth;
import kaptainwutax.minemap.MineMap;
import kaptainwutax.minemap.init.Configs;
import kaptainwutax.minemap.listener.Events;
import kaptainwutax.minemap.ui.dialog.RenameTabDialog;
import kaptainwutax.minemap.ui.map.tool.Area;
import kaptainwutax.minemap.ui.map.tool.Circle;
import kaptainwutax.minemap.ui.map.tool.Ruler;
import kaptainwutax.minemap.ui.map.tool.Tool;
import kaptainwutax.seedutils.mc.pos.BPos;
import kaptainwutax.seedutils.util.math.Vec3i;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapManager {

    public static final int DEFAULT_REGION_SIZE = 512;

    private final MapPanel panel;
    public final int blocksPerFragment;
    public double pixelsPerFragment;

    public double centerX;
    public double centerY;

    public final ArrayList<Tool> toolsList = new ArrayList<>();
    public Tool selectedTool = null;

    public Point mousePointer;

    public MapManager(MapPanel panel) {
        this(panel, DEFAULT_REGION_SIZE);
    }

    public MapManager(MapPanel panel, int blocksPerFragment) {
        this.panel = panel;
        this.blocksPerFragment = blocksPerFragment;
        this.pixelsPerFragment = (int) (256.0D * (this.blocksPerFragment / DEFAULT_REGION_SIZE));

        this.panel.addMouseMotionListener(Events.Mouse.onDragged(e -> {
            if (SwingUtilities.isLeftMouseButton(e)) {
                int dx = e.getX() - this.mousePointer.x;
                int dy = e.getY() - this.mousePointer.y;
                this.mousePointer = e.getPoint();
                this.centerX += dx;
                this.centerY += dy;
                this.panel.repaint();
            }
        }));

        this.panel.addMouseMotionListener(Events.Mouse.onMoved(e -> {
            BPos pos = this.getPos(e.getX(), e.getY());
            int x = pos.getX();
            int z = pos.getZ();
            this.panel.scheduler.forEachFragment(fragment -> fragment.onHovered(pos.getX(), pos.getZ()));

            SwingUtilities.invokeLater(() -> {
                this.panel.leftBar.tooltip.updateBiomeDisplay(x, z);
                this.panel.leftBar.tooltip.tooltip.repaint();
                this.panel.repaint();
            });
        }));

        this.panel.addMouseListener(Events.Mouse.onPressed(e -> {
            if (SwingUtilities.isLeftMouseButton(e)) {
                this.mousePointer = e.getPoint();
                if (selectedTool == null) {
                    this.panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    BPos pos = this.getPos(e.getX(), e.getY());
                    // if tool has no more points to it
                    if (!selectedTool.addPoint(pos)) {
                        selectedTool = selectedTool.duplicate();
                        toolsList.add(selectedTool);
                        selectedTool.addPoint(pos);
                    }
                    this.panel.rightBar.tooltip.updateToolsMetrics(toolsList);
                }
            }
        }));

        this.panel.addMouseListener(Events.Mouse.onReleased(e -> {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (selectedTool == null) {
                    this.panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        }));

        this.panel.addMouseWheelListener(e -> {
            if (!e.isControlDown()) {
                double newPixelsPerFragment = this.pixelsPerFragment;

                if (e.getUnitsToScroll() > 0) {
                    newPixelsPerFragment /= 2.0D;
                } else {
                    newPixelsPerFragment *= 2.0D;
                }

                if (newPixelsPerFragment > 2048.0D * (double) this.blocksPerFragment / DEFAULT_REGION_SIZE) {
                    newPixelsPerFragment = 2048.0D * (this.blocksPerFragment / 512.0D);
                }

                if (Configs.USER_PROFILE.getUserSettings().restrictMaximumZoom
                        && newPixelsPerFragment < 32.0D * (double) this.blocksPerFragment / DEFAULT_REGION_SIZE) {
                    newPixelsPerFragment = 32.0D * (this.blocksPerFragment / 512.0D);
                }

                double scaleFactor = newPixelsPerFragment / this.pixelsPerFragment;
                this.centerX *= scaleFactor;
                this.centerY *= scaleFactor;
                this.pixelsPerFragment = newPixelsPerFragment;
                this.panel.repaint();
            } else {
                int layerId = this.panel.getContext().getLayerId();
                layerId += e.getUnitsToScroll() < 0 ? 1 : -1;
                layerId = Mth.clamp(layerId, 0, this.panel.getContext().getBiomeSource().getLayerCount() - 1);

                if (this.panel.getContext().getLayerId() != layerId) {
                    this.panel.getContext().setLayerId(layerId);
                    this.panel.leftBar.settings.layerDropdown.selectIfPresent(layerId);
                    this.panel.restart();
                }
            }
        });

        JPopupMenu popup = new JPopupMenu();

        JMenuItem pin = new JMenuItem("Pin");
        pin.setBorder(new EmptyBorder(5, 15, 5, 15));

        pin.addMouseListener(Events.Mouse.onReleased(e -> {
            boolean newState = !MineMap.INSTANCE.worldTabs.getSelectedHeader().isPinned();
            MineMap.INSTANCE.worldTabs.getSelectedHeader().setPinned(newState);
            pin.setText(newState ? "Unpin" : "Pin");
        }));

        JMenuItem rename = new JMenuItem("Rename");
        rename.setBorder(new EmptyBorder(5, 15, 5, 15));

        rename.addMouseListener(Events.Mouse.onReleased(e -> {
            RenameTabDialog renameTabDialog = new RenameTabDialog();
            renameTabDialog.setVisible(true);
        }));

        JMenuItem settings = new JMenuItem("Settings");
        settings.setBorder(new EmptyBorder(5, 15, 5, 15));

        settings.addMouseListener(Events.Mouse.onReleased(e -> {
            this.panel.leftBar.settings.setVisible(!panel.leftBar.settings.isVisible());
        }));

        popup.add(pin);
        popup.add(rename);
        popup.add(settings);
        this.addTools(popup, Arrays.asList(Ruler::new,  Area::new,  Circle::new));

        this.panel.setComponentPopupMenu(popup);
    }

    public void addTools(JPopupMenu popup,List<Supplier<Tool>> tools){
        List<JMenuItem> toolMenus=new ArrayList<>();
        for (int i = 0; i < tools.size(); i++) {
            JMenuItem toolMenu=new JMenuItem();
            toolMenu.setBorder(new EmptyBorder(5, 15, 5, 15));
            toolMenus.add(toolMenu);
        }
        Consumer<String> rTools = prefix -> {
            for (int i = 0; i < tools.size(); i++) {
                toolMenus.get(i).setText(String.join(" ", prefix, tools.get(i).get().getName()));
            }
        };
        rTools.accept("Enable");

        BiConsumer<Tool, JMenuItem> createNewTool = (newTool, menuItem) -> {
            toolsList.add(newTool);
            selectedTool = newTool;
            this.panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            menuItem.setText("Disable " + newTool.getName());
        };

        BiConsumer<Supplier<Tool>, JMenuItem> toggleTool = (newTool, menuItem) -> {
            Tool tool = newTool.get(); // to avoid creating an instance at one point
            if (selectedTool == null) {
                createNewTool.accept(tool, menuItem);
            } else {
                this.panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                rTools.accept("Enable");
                if (!selectedTool.isAcceptable()) {
                    removeTool(selectedTool);
                }
                if (tool.getClass().equals(selectedTool.getClass())) {
                    selectedTool = null;
                } else {
                    createNewTool.accept(tool, menuItem);
                }
            }
        };

        for (int i = 0; i < tools.size(); i++) {
            JMenuItem toolMenu=toolMenus.get(i);
            Supplier<Tool> tool=tools.get(i);
            toolMenu.addMouseListener(Events.Mouse.onReleased(e -> toggleTool.accept(tool, toolMenu)));
            popup.add(toolMenu);
        }
    }

    public void removeTool(Tool tool) {
        if (selectedTool == tool) {
            selectedTool = tool.duplicate();
            selectedTool.reset();
        }
        if (!toolsList.remove(tool)) {
            System.out.println("This is unexpected");
        }
        this.panel.rightBar.tooltip.updateToolsMetrics(toolsList);
    }

    public Vec3i getScreenSize() {
        return new Vec3i(this.panel.getWidth(), 0, this.panel.getHeight());
    }

    public BPos getCenterPos() {
        Vec3i screenSize = this.getScreenSize();
        return getPos(screenSize.getX() / 2.0D, screenSize.getZ() / 2.0D);
    }

    public void setCenterPos(int blockX, int blockZ) {
        double scaleFactor = this.pixelsPerFragment / this.blocksPerFragment;
        this.centerX = -blockX * scaleFactor;
        this.centerY = -blockZ * scaleFactor;
        this.panel.repaint();
    }

    public BPos getPos(double mouseX, double mouseY) {
        Vec3i screenSize = this.getScreenSize();
        double x = (mouseX - screenSize.getX() / 2.0D - centerX) / screenSize.getX();
        double y = (mouseY - screenSize.getZ() / 2.0D - centerY) / screenSize.getZ();
        double blocksPerWidth = (screenSize.getX() / this.pixelsPerFragment) * (double) this.blocksPerFragment;
        double blocksPerHeight = (screenSize.getZ() / this.pixelsPerFragment) * (double) this.blocksPerFragment;
        x *= blocksPerWidth;
        y *= blocksPerHeight;
        int xi = (int) Math.round(x);
        int yi = (int) Math.round(y);
        return new BPos(xi, 0, yi);
    }

}
