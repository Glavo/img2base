package org.glavo.img2base;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {
    public static final int DEFAULT_WIDTH = 600;
    public static final int DEFAULT_HEIGHT = 400;

    public static final DateFormat format = new SimpleDateFormat("HH:mm:ss");
    public static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        initLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            final JFrame window = buildMainWindow();
            window.setVisible(true);
        });
    }

    private static void initLookAndFeel() {
        if (System.getProperty("swing.defaultlaf") == null) {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static JFrame buildMainWindow() {
        final JFrame window = new JFrame();

        window.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setLocationByPlatform(true);
        window.setTitle("Image to Base64");

        final JLabel messageBar = new JLabel("Drag in the picture for conversion, and click the right mouse button to copy");
        window.add(BorderLayout.SOUTH, messageBar);


        final JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    if (support.getUserDropAction() == TransferHandler.MOVE) {
                        support.setDropAction(TransferHandler.COPY);
                    }
                    return true;
                }
                return support.isDataFlavorSupported(DataFlavor.imageFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                final Transferable transferable = support.getTransferable();
                final List<DataFlavor> flavors = Arrays.asList(transferable.getTransferDataFlavors());

                final Object fileOrImage;

                try {
                    if (flavors.contains(DataFlavor.javaFileListFlavor)) {
                        final List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (fileList.isEmpty()) {
                            SwingUtilities.invokeLater(() -> {
                                showErrorDialog(window, "file list is empty");
                                area.setText("");
                                messageBar.setText(message("File list is empty"));
                            });
                            return false;
                        }
                        fileOrImage = fileList.get(0);
                    } else if (flavors.contains(DataFlavor.imageFlavor)) {
                        fileOrImage = transferable.getTransferData(DataFlavor.imageFlavor);
                    } else {
                        return false;
                    }

                } catch (IOException | UnsupportedFlavorException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showErrorDialog(window, ex);
                        messageBar.setText("Error: " + ex);
                    });
                    return false;
                }

                executorService.submit(() -> {
                    try {
                        final byte[] data;
                        if (fileOrImage instanceof File) {
                            data = Files.readAllBytes(((File) fileOrImage).toPath());
                        } else if (fileOrImage instanceof Image) {
                            Image image = (Image) fileOrImage;
                            if (!(image instanceof RenderedImage)) {
                                final BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                                Graphics2D bGr = bi.createGraphics();
                                bGr.drawImage(image, 0, 0, null);
                                bGr.dispose();
                                image = bi;
                            }

                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                ImageIO.write((RenderedImage) image, "jpeg", out);
                                data = out.toByteArray();
                            }
                        } else {
                            throw new AssertionError();
                        }
                        //noinspection StringBufferReplaceableByString
                        final StringBuilder builder = new StringBuilder(27 + data.length * 2);
                        builder.append("![](data:image/png;base64,")
                                .append(Base64.getEncoder().encodeToString(data))
                                .append(")");
                        final String res = builder.toString();
                        SwingUtilities.invokeLater(() -> {
                            area.setText(res);
                            messageBar.setText(message("Encoding succeeded"));
                        });
                    } catch (Throwable ex) {
                        SwingUtilities.invokeLater(() -> {
                            showErrorDialog(window, ex);
                            area.setText("");
                            messageBar.setText("Error: " + ex);
                        });
                    }
                });

                return true;
            }
        });
        area.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(area.getText()), null);
                    messageBar.setText(message("Copy to clipboard"));
                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    area.setText("");
                    messageBar.setText(message("Clear text"));
                }
            }
        });
        final JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setAutoscrolls(false);
        window.add(BorderLayout.CENTER, scrollPane);
        return window;
    }

    private static String message(String message) {
        return format.format(new Date()) + ": " + message;
    }

    private static void showErrorDialog(JFrame frame, Object message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }
}
