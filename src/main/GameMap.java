package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameMap {
    public static final int TILE_SIZE = 50;
    public static final int COLS = 30;
    public static final int ROWS = 15;

    // Palet Dark Minimalist
    private static final Color BG_GRASS = new Color(24, 27, 32);
    private static final Color PATH_COLOR = new Color(38, 43, 52);
    private static final Color PATH_CENTER_LINE = new Color(52, 59, 71);
    private static final Color GRID_LINE = new Color(33, 37, 45);
    private static final Color SPAWN_ACCENT = new Color(201, 93, 86);
    private static final Color BASE_ACCENT = new Color(78, 137, 174);

    private final int[][] grid;
    private final List<Point> waypoints;
    private final Random random;

    public GameMap() {
        this.grid = new int[ROWS][COLS];
        this.waypoints = new ArrayList<>();
        this.random = new Random();
        generateRandomPath();
    }

    public final void generateRandomPath() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = 0;
            }
        }
        waypoints.clear();

        List<Point> gridPoints = new ArrayList<>();
        int minRow = ROWS >= 5 ? 1 : 0;
        int maxRow = ROWS >= 5 ? ROWS - 2 : ROWS - 1;

        int curCol = 0;
        int curRow = minRow + random.nextInt(Math.max(1, maxRow - minRow + 1));
        gridPoints.add(new Point(curCol, curRow));

        // Langkah awal masuk ke dalam peta dari tepi kiri
        int minStep = COLS >= 10 ? 2 : 1;
        int maxStep = Math.max(minStep, COLS / 4);
        int firstCol = Math.min(COLS - 2, curCol + minStep + random.nextInt(2));
        curCol = firstCol;
        gridPoints.add(new Point(curCol, curRow));

        // Belok vertikal & maju horizontal secara bergantian hingga mencapai COLS - 1
        while (curCol < COLS - 2) {
            int midRow = (minRow + maxRow) / 2;
            int targetRow;
            if (curRow <= midRow) {
                int lowStart = Math.min(maxRow, midRow + 1);
                targetRow = lowStart + random.nextInt(Math.max(1, maxRow - lowStart + 1));
            } else {
                int highEnd = Math.max(minRow, midRow - 1);
                targetRow = minRow + random.nextInt(Math.max(1, highEnd - minRow + 1));
            }

            if (targetRow != curRow) {
                curRow = targetRow;
                gridPoints.add(new Point(curCol, curRow));
            }

            int remainingCols = (COLS - 1) - curCol;
            int step = minStep + random.nextInt(Math.max(1, maxStep - minStep + 1));
            int nextCol = curCol + step;

            // Jika sisa kolom tinggal sedikit, langsung sambungkan ke tepi kanan
            if (nextCol >= COLS - 2 || remainingCols <= minStep + 1) {
                nextCol = COLS - 1;
            }

            curCol = nextCol;
            gridPoints.add(new Point(curCol, curRow));
        }

        if (curCol < COLS - 1) {
            gridPoints.add(new Point(COLS - 1, curRow));
        }

        // Ukir jalur pada grid berdasarkan titik belokan
        for (int i = 0; i < gridPoints.size() - 1; i++) {
            Point p1 = gridPoints.get(i);
            Point p2 = gridPoints.get(i + 1);

            for (int r = Math.min(p1.y, p2.y); r <= Math.max(p1.y, p2.y); r++) {
                for (int c = Math.min(p1.x, p2.x); c <= Math.max(p1.x, p2.x); c++) {
                    grid[r][c] = 1;
                }
            }
        }

        Point start = gridPoints.get(0);
        Point end = gridPoints.get(gridPoints.size() - 1);
        grid[start.y][start.x] = 2; // Spawn (IN)
        grid[end.y][end.x] = 3;     // Base

        // Konversi titik grid ke koordinat piksel tengah
        for (Point gp : gridPoints) {
            int px = gp.x * TILE_SIZE + TILE_SIZE / 2;
            int py = gp.y * TILE_SIZE + TILE_SIZE / 2;
            waypoints.add(new Point(px, py));
        }
    }

    public boolean isBuildable(int col, int row) {
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return false;
        }
        return grid[row][col] == 0;
    }

    public List<Point> getWaypoints() {
        return waypoints;
    }

    public void draw(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1f));

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;
                int cell = grid[r][c];

                g2.setColor(cell == 0 ? BG_GRASS : PATH_COLOR);
                g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);

                if (cell == 0) {
                    g2.setColor(GRID_LINE);
                    g2.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                }

                if (cell == 2 || cell == 3) {
                    g2.setColor(cell == 2 ? SPAWN_ACCENT : BASE_ACCENT);
                    g2.fillRoundRect(x + 8, y + 14, TILE_SIZE - 16, TILE_SIZE - 28, 10, 10);

                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    String label = cell == 2 ? "IN" : "BASE";
                    g2.drawString(label, x + (cell == 2 ? 19 : 11), y + 29);
                }
            }
        }

        // Garis penunjuk arah tipis di tengah jalur
        float[] dash = {4f, 6f};
        g2.setColor(PATH_CENTER_LINE);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Point a = waypoints.get(i);
            Point b = waypoints.get(i + 1);
            g2.drawLine(a.x, a.y, b.x, b.y);
        }
    }
}