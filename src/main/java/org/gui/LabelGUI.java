package org.gui;

import org.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


    public class LabelGUI extends JFrame {
        private AtomicReference<Float> latitudeRef;
        private AtomicReference<Float> longitudeRef;
        private float currentX;
        private float currentY;
        private final float centerLongitude;
        private final float centerLatitude;
        private ConfigHandler config;
        private BufferedImage originalImage;
        private int stepSize = 5;
        private JLabel coordinatesLabel;

        private enum Direction {
            Xmin, Xplus, Ymin, Yplus;
        }

        public LabelGUI(ConfigHandler config, float longitude, float latitude) {
            centerLongitude = longitude;
            centerLatitude = latitude;

            longitudeRef = new AtomicReference<>(longitude);
            latitudeRef = new AtomicReference<>(latitude);

            this.config = config;

            String[] buttons = config.getProperty("app.chooseLabels").split(",");

            int size = Integer.parseInt(config.getProperty("app.imageWidth"));
            currentX = size / 2;
            currentY = size / 2;
            List<String> labelMapping = List.of(buttons);
            JLabel imgLabel = createImgLabel(longitudeRef.get(), latitudeRef.get(), size);
            JLabel mainPanel = new JLabel();
            JLabel headerLabel = new JLabel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            JPanel contentPanel = new JPanel();
            contentPanel.setBackground(Color.WHITE);
            contentPanel.setLayout(new GridBagLayout());

            contentPanel.add(imgLabel, new GridBagConstraints());

            JPanel controlButtonPanel = new JPanel();
            controlButtonPanel.setLayout(new BoxLayout(controlButtonPanel, BoxLayout.Y_AXIS));
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());

            JButton plusButtonX = new JButton("X+");
            JButton minusButtonX = new JButton("X-");
            JButton plusButtonY = new JButton("Y+");
            JButton minusButtonY = new JButton("Y-");

            plusButtonY.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveCircle(imgLabel, size, Direction.Ymin);
                }
            });

            plusButtonX.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveCircle(imgLabel, size, Direction.Xplus);
                }
            });

            minusButtonX.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveCircle(imgLabel, size, Direction.Xmin);
                }
            });

            minusButtonY.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveCircle(imgLabel, size, Direction.Yplus);
                }
            });

            JButton copyButton = new JButton("Copy");
            copyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String coordinates = longitudeRef.get() + "," + latitudeRef.get();
                    StringSelection stringSelection = new StringSelection(coordinates);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                }
            });

            imgLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    currentX = (float) e.getX();
                    currentY = (float) e.getY();
                    float relativeSize = Float.parseFloat(config.getProperty("app.relativeWidth"));
                    List<Float> geoCoordinates = CoordinatesHandler.convertToGeoCoordinates(currentX, currentY,
                            centerLongitude, centerLatitude, size, relativeSize);
                    longitudeRef.set(geoCoordinates.get(0));
                    latitudeRef.set(geoCoordinates.get(1));
                    drawRedCircle(imgLabel, (int)currentX, (int)currentY, size);
                    System.out.println("Longitude, Latitude: " + geoCoordinates.get(0) + "," + geoCoordinates.get(1));
                    updateCoordinatesLabel();
                }
            });

            buttonPanel.add(plusButtonX);
            buttonPanel.add(minusButtonX);
            buttonPanel.add(plusButtonY);
            buttonPanel.add(minusButtonY);

            // Add step size slider
            JPanel sliderPanel = new JPanel();
            sliderPanel.setLayout(new FlowLayout());
            JSlider stepSizeSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, stepSize);
            stepSizeSlider.setMajorTickSpacing(5);
            stepSizeSlider.setMinorTickSpacing(1);
            stepSizeSlider.setPaintTicks(true);
            stepSizeSlider.setPaintLabels(true);
            stepSizeSlider.addChangeListener(e -> stepSize = stepSizeSlider.getValue());
            stepSizeSlider.setPreferredSize(new Dimension(stepSizeSlider.getPreferredSize().width * 2, stepSizeSlider.getPreferredSize().height));
            sliderPanel.add(new JLabel("Schrittgröße:"));
            sliderPanel.add(stepSizeSlider);



            controlButtonPanel.add(buttonPanel);
            controlButtonPanel.add(sliderPanel);

            JPanel coordinatesPanel = new JPanel();
            coordinatesLabel = new JLabel();
            updateCoordinatesLabel();
            JButton googleMapsButton = new JButton("Open in Google Maps");
            googleMapsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://www.google.com/maps/search/?api=1&query=" + longitudeRef.get() + "," + latitudeRef.get()));
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            coordinatesPanel.add(copyButton);
            coordinatesPanel.add(coordinatesLabel);
            coordinatesPanel.add(googleMapsButton);

            controlButtonPanel.add(coordinatesPanel);

            JPanel labelButtonPanel = new JPanel();
            for (String button : buttons) {
                JButton labelButton = new JButton(button);
                String label = button;
                labelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String[] values = {String.valueOf(longitudeRef.get()), String.valueOf(latitudeRef.get()), label};
                        CsvHandler.writeToCSV(values, config.getProperty("app.storeCSV"));
                        LabelGUI.super.dispose();
                    }
                });
                labelButtonPanel.add(labelButton, new GridBagConstraints());
            }

            JPanel exitPanel = new JPanel();
            JButton exitButton = new JButton("EXIT");
            exitButton.setBackground(Color.RED);
            exitButton.setSize(new Dimension(100, 50));
            exitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            exitPanel.add(exitButton);

            mainPanel.add(headerLabel);
            mainPanel.add(controlButtonPanel);
            mainPanel.add(contentPanel);
            mainPanel.add(labelButtonPanel);
            mainPanel.add(exitPanel);
            this.setSize(new Dimension(700, 700));
            this.setTitle("Labelling");

            this.add(mainPanel);
            this.setResizable(true);
            this.setVisible(true);
            this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
        }

        private Image getCurrentImage(float longitude, float latitude) throws IOException {
            String url = config.getProperty("app.formatURL");
            String size = config.getProperty("app.imageWidth");
            float relativeSize = Float.parseFloat(config.getProperty("app.relativeWidth"));
            ArrayList<Float> bbox = (ArrayList<Float>) CoordinatesHandler.formBbox(longitude, latitude, relativeSize);
            String filledUrl = RequestHandler.fillURL(url, bbox, size);
            return RequestHandler.getImageFromURL(filledUrl);
        }

        private JLabel createImgLabel(float longitude, float latitude, int size) {
            Image image;
            try {
                image = getCurrentImage(longitude, latitude);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            originalImage = ImageUtils.toBufferedImage(image);
            Image modifiedImage;
            if (longitude == 0 && latitude == 0) {
                modifiedImage = image;
            } else {
                modifiedImage = ImageUtils.addCenterLabel(originalImage, size / 2, size / 2);
            }

            ImageIcon img = new ImageIcon(modifiedImage);
            JLabel imgLabel = new JLabel();
            imgLabel.setPreferredSize(new Dimension(size, size));
            imgLabel.setMaximumSize(new Dimension(size, size));
            imgLabel.setMinimumSize(new Dimension(size, size));
            imgLabel.setIcon(img);

            return imgLabel;
        }

        private void updateImage(JLabel imgLabel, int size) {
            Image newImage;
            try {
                newImage = getCurrentImage(longitudeRef.get(), latitudeRef.get());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            originalImage = ImageUtils.toBufferedImage(newImage);
            Image modifiedImage = ImageUtils.addCenterLabel(originalImage, size / 2, size / 2);
            ImageIcon icon = new ImageIcon(modifiedImage);
            imgLabel.setIcon(icon);
            imgLabel.revalidate();
            imgLabel.repaint();
        }

        private void drawRedCircle(JLabel imgLabel, int x, int y, int size) {
            BufferedImage bufferedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.setColor(Color.RED);
            g2d.fillOval(x - 5, y - 5, 10, 10);  // Drawing a red circle with radius 5
            g2d.dispose();

            ImageIcon newIcon = new ImageIcon(bufferedImage);
            imgLabel.setIcon(newIcon);
            imgLabel.revalidate();
            imgLabel.repaint();
        }

        private void moveCircle(JLabel imgLabel, int size, Direction direction) {
            float relativeSize = Float.parseFloat(config.getProperty("app.relativeWidth"));
            switch (direction) {
                case Xmin:
                    currentX = currentX - stepSize;
                    break;
                case Xplus:
                    currentX = currentX + stepSize;
                    break;
                case Ymin:
                    currentY = currentY - stepSize;
                    break;
                case Yplus:
                    currentY = currentY + stepSize;
                    break;
            }
            List<Float> geoCoordinates = CoordinatesHandler
                    .convertToGeoCoordinates(currentX, currentY, centerLongitude,
                            centerLatitude, size, relativeSize);
            drawRedCircle(imgLabel, (int)currentX, (int)currentY, size);
            System.out.println("Longitude, Latitude: " + geoCoordinates.get(0) + "," + geoCoordinates.get(1));
            latitudeRef.set(geoCoordinates.get(1));
            updateCoordinatesLabel();
        }

        private void updateCoordinatesLabel() {
            coordinatesLabel.setText("Longitude, Latitude: " + longitudeRef.get() + "," + latitudeRef.get());
        }

}

