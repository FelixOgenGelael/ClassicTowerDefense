package main;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    private static JFrame frame;
    private static GamePanel gamePanel;
    private static boolean fullScreen = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Java Tower Defense");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);

            gamePanel = new GamePanel();
            frame.add(gamePanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void toggleFullScreen() {
        setFullScreen(!fullScreen);
    }

    public static boolean isFullScreen() {
        return fullScreen;
    }

    public static void setFullScreen(boolean enable) {
        if (fullScreen == enable || frame == null) return;
        fullScreen = enable;

        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        // Dispose wajib dipanggil sebelum mengubah setUndecorated saat runtime
        frame.dispose();
        frame.setUndecorated(fullScreen);

        if (fullScreen) {
            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(frame);
            } else {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        } else {
            if (gd.getFullScreenWindow() == frame) {
                gd.setFullScreenWindow(null);
            }
            frame.setExtendedState(JFrame.NORMAL);
            frame.pack();
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        gamePanel.requestFocusInWindow();
    }
}