package kaptainwutax.minemap.ui.map;

import kaptainwutax.biomeutils.Biome;
import kaptainwutax.biomeutils.layer.BiomeLayer;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.Feature;
import kaptainwutax.minemap.MineMap;
import kaptainwutax.minemap.listener.Events;
import kaptainwutax.minemap.ui.component.Dropdown;
import kaptainwutax.seedutils.mc.pos.BPos;
import kaptainwutax.seedutils.mc.pos.RPos;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.stream.IntStream;

public class MapDisplayBar extends JPanel {

    private final MapPanel panel;

    private JLabel biomeDisplay;
    private Dropdown<Integer> layerDropdown;
    private JButton pinButton;
    private ScrollPane scrollPane;

    public MapDisplayBar(MapPanel panel) {
        this.panel = panel;
        this.addPinButton();
        this.addBiomeDisplay();
        this.addLayerDropdown();
        this.addFeatureToggles();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    private void addFeatureToggles() {
        this.scrollPane = new ScrollPane();
        this.scrollPane.getVAdjustable().setUnitIncrement(40);

        JPanel toggles = new JPanel();
        this.scrollPane.add(toggles);

        toggles.setLayout(new BoxLayout(toggles, BoxLayout.Y_AXIS));

        MapSettings settings = this.panel.getContext().getSettings();

        for(Feature<?, ?> feature: settings.getAllFeatures()) {
            Checkbox checkBox = new Checkbox(feature.getName());
            checkBox.setState(settings.isActive(feature));

            checkBox.addItemListener(e -> {
                settings.setState(feature, checkBox.getState());
                this.panel.repaint();
            });

            toggles.add(checkBox, Component.CENTER_ALIGNMENT);
        }

        for(Biome biome: settings.getAllBiomes()) {
            Checkbox checkBox = new Checkbox(biome.getName());
            checkBox.setState(settings.isActive(biome));

            checkBox.addItemListener(e -> {
                settings.setState(biome, checkBox.getState());
                this.panel.repaint();
            });

            toggles.add(checkBox, Component.CENTER_ALIGNMENT);
        }

        JButton hideAll = new JButton("Hide All");
        JButton showAll = new JButton("Show All");

        hideAll.addMouseListener(Events.Mouse.onPressed(e -> {
            settings.getAllBiomes().forEach(settings::hide);
            settings.getAllFeatures().forEach(f -> settings.setState(f, false));
            Arrays.stream(toggles.getComponents()).filter(c -> c instanceof Checkbox).map(c -> (Checkbox)c).forEach(c -> c.setState(false));
            this.panel.repaint();
        }));

        showAll.addMouseListener(Events.Mouse.onPressed(e -> {
            settings.getAllBiomes().forEach(settings::show);
            settings.getAllFeatures().forEach(f -> settings.setState(f, true));
            Arrays.stream(toggles.getComponents()).filter(c -> c instanceof Checkbox).map(c -> (Checkbox)c).forEach(c -> c.setState(true));
            this.panel.repaint();
        }));

        hideAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        showAll.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(this.scrollPane);
        this.add(hideAll);
        this.add(showAll);
    }

    private void addBiomeDisplay() {
        this.biomeDisplay = new JLabel();
        this.biomeDisplay.setFocusable(false);
        this.biomeDisplay.setOpaque(true);
        this.biomeDisplay.setVerticalAlignment(SwingConstants.TOP);
        this.biomeDisplay.setHorizontalAlignment(SwingConstants.LEFT);
        //this.biomeDisplay.setBackground(new Color(0, 0, 0, 127));
        this.biomeDisplay.setForeground(Color.WHITE);
        this.biomeDisplay.setHorizontalTextPosition(SwingConstants.LEFT);
        //this.biomeDisplay.setFont(new Font(".SF NS Text", Font.BOLD, 14));
        this.biomeDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(this.biomeDisplay);
    }

    private void addLayerDropdown() {
        BiomeSource source = this.panel.getContext().getBiomeSource();
        this.layerDropdown = new Dropdown<>(i -> "[" + i + "] " + source.getLayer(i).getClass().getSimpleName() + " " + source.getLayer(i).getScale() + ":1", IntStream.range(0, source.getLayerCount()).boxed());
        this.layerDropdown.selectIfPresent(this.panel.getContext().getLayerId());

        this.layerDropdown.addActionListener(e1 -> {
            this.panel.getContext().setLayerId(this.layerDropdown.getSelected());
            this.panel.restart();
        });

        this.layerDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(this.layerDropdown);
    }

    private void addPinButton() {
        this.pinButton = new JButton("Pin");

        this.pinButton.addMouseListener(Events.Mouse.onPressed(e -> {
            boolean newState = !MineMap.INSTANCE.worldTabs.getSelectedHeader().isPinned();
            MineMap.INSTANCE.worldTabs.getSelectedHeader().setPinned(newState);
            this.pinButton.setText(newState ? "Unpin" : "Pin");
        }));

        this.pinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(this.pinButton);
    }

    public void updateBiomeDisplay(int blockX, int blockZ) {
        int biomeId = this.getBiome(blockX, blockZ);
        Biome biome = Biome.REGISTRY.get(biomeId);
        String name = biome == null ? "UNKNOWN" : biome.getName().toUpperCase();

        String text = String.format("(%d, %d): %s with ID %d (0x%s)", blockX, blockZ, name,
                biomeId, Integer.toHexString(biomeId).toUpperCase());
        this.biomeDisplay.setText(text);
    }

    private int getBiome(int blockX, int blockZ) {
        BiomeLayer layer = this.panel.getContext().getBiomeLayer();
        RPos pos = new BPos(blockX, 0, blockZ).toRegionPos(layer.getScale());
        return layer.get(pos.getX(), 0, pos.getZ());
    }

}
