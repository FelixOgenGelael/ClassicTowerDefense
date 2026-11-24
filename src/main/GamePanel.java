package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int MAP_HEIGHT = GameMap.ROWS * GameMap.TILE_SIZE;
    private static final int HUD_HEIGHT = 110;
    private static final int WIDTH = GameMap.COLS * GameMap.TILE_SIZE;
    private static final int HEIGHT = MAP_HEIGHT + HUD_HEIGHT;

    // Palet Dark Minimalist
    private static final Color OUTER_BG = new Color(17, 19, 23);
    private static final Color HUD_BG = new Color(20, 23, 28);
    private static final Color CARD_BG = new Color(28, 32, 40);
    private static final Color BTN_DISABLED_BG = new Color(24, 27, 33);
    private static final Color BORDER_MUTED = new Color(45, 51, 63);
    private static final Color TEXT_PRIMARY = new Color(236, 238, 242);
    private static final Color TEXT_SECONDARY = new Color(138, 146, 160);
    private static final Color ACCENT_UPGRADE = new Color(91, 185, 116);

    private GameMap map;
    private List<Tower> towers;
    private List<Enemy> enemies;
    private List<Projectile> projectiles;
    private List<ExplosionEffect> explosions;

    private int gold = 150;
    private int lives = 20;
    private int wave = 0;

    private boolean waveActive = false;
    private int enemiesToSpawn = 0;
    private int spawnCooldown = 0;
    private int spawnedInWave = 0;

    private Tower.Type selectedBuildType = Tower.Type.ARCHER;
    private Tower selectedTower = null;
    private int hoverCol = -1;
    private int hoverRow = -1;
    private boolean gameOver = false;

    // Tombol HUD
    private final Rectangle btnRerollMap = new Rectangle(610, MAP_HEIGHT + 11, 86, 24);
    private final Rectangle btnFullScreen = new Rectangle(704, MAP_HEIGHT + 11, 72, 24);
    private final Rectangle btnArcher = new Rectangle(24, MAP_HEIGHT + 44, 112, 48);
    private final Rectangle btnIce = new Rectangle(146, MAP_HEIGHT + 44, 112, 48);
    private final Rectangle btnCannon = new Rectangle(268, MAP_HEIGHT + 44, 112, 48);
    private final Rectangle btnUpgrade = new Rectangle(412, MAP_HEIGHT + 44, 112, 48);
    private final Rectangle btnSell = new Rectangle(534, MAP_HEIGHT + 44, 96, 48);
    private final Rectangle btnWave = new Rectangle(648, MAP_HEIGHT + 44, 128, 48);

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(OUTER_BG);
        setFocusable(true);
        resetGame();
        setupInput();
        setupKeyBindings();

        Timer timer = new Timer(16, e -> {
            updateGame();
            repaint();
        });
        timer.start();
    }

    private void resetGame() {
        map = new GameMap();
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        explosions = new ArrayList<>();

        gold = 150;
        lives = 20;
        wave = 0;
        waveActive = false;
        enemiesToSpawn = 0;
        selectedTower = null;
        gameOver = false;
    }

    private double getScale() {
        if (getWidth() <= 0 || getHeight() <= 0) return 1.0;
        return Math.min((double) getWidth() / WIDTH, (double) getHeight() / HEIGHT);
    }

    private int getOffsetX(double scale) {
        return (int) Math.round((getWidth() - WIDTH * scale) / 2.0);
    }

    private int getOffsetY(double scale) {
        return (int) Math.round((getHeight() - HEIGHT * scale) / 2.0);
    }

    private int toLogicalX(int screenX) {
        double scale = getScale();
        return (int) Math.floor((screenX - getOffsetX(scale)) / scale);
    }

    private int toLogicalY(int screenY) {
        double scale = getScale();
        return (int) Math.floor((screenY - getOffsetY(scale)) / scale);
    }

    private void setupKeyBindings() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullScreen");
        getActionMap().put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.toggleFullScreen();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitFullScreen");
        getActionMap().put("exitFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Main.isFullScreen()) {
                    Main.setFullScreen(false);
                } else {
                    selectedTower = null;
                }
            }
        });
    }

    private void setupInput() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int lx = toLogicalX(e.getX());
                int ly = toLogicalY(e.getY());

                if (lx >= 0 && lx < WIDTH && ly >= 0 && ly < MAP_HEIGHT) {
                    hoverCol = lx / GameMap.TILE_SIZE;
                    hoverRow = ly / GameMap.TILE_SIZE;
                } else {
                    hoverCol = -1;
                    hoverRow = -1;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int mx = toLogicalX(e.getX());
                int my = toLogicalY(e.getY());

                if (mx < 0 || mx >= WIDTH || my < 0 || my >= HEIGHT) {
                    return;
                }

                if (gameOver) {
                    if (btnFullScreen.contains(mx, my)) {
                        Main.toggleFullScreen();
                    } else {
                        resetGame();
                    }
                    return;
                }

                if (my >= MAP_HEIGHT) {
                    handleHudClick(mx, my);
                } else {
                    handleMapClick(mx / GameMap.TILE_SIZE, my / GameMap.TILE_SIZE);
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void handleHudClick(int mx, int my) {
        if (btnFullScreen.contains(mx, my)) {
            Main.toggleFullScreen();
        } else if (btnRerollMap.contains(mx, my) && wave == 0 && !waveActive) {
            // Acak ulang jalur & kembalikan koin tower yang terpasang
            for (Tower t : towers) {
                gold += t.getType().cost;
            }
            towers.clear();
            selectedTower = null;
            map.generateRandomPath();
        } else if (btnArcher.contains(mx, my)) {
            selectedBuildType = Tower.Type.ARCHER;
            selectedTower = null;
        } else if (btnIce.contains(mx, my)) {
            selectedBuildType = Tower.Type.ICE;
            selectedTower = null;
        } else if (btnCannon.contains(mx, my)) {
            selectedBuildType = Tower.Type.CANNON;
            selectedTower = null;
        } else if (btnUpgrade.contains(mx, my) && selectedTower != null) {
            int cost = selectedTower.getUpgradeCost();
            if (gold >= cost) {
                gold -= cost;
                selectedTower.upgrade();
            }
        } else if (btnSell.contains(mx, my) && selectedTower != null) {
            gold += selectedTower.getSellValue();
            towers.remove(selectedTower);
            selectedTower = null;
        } else if (btnWave.contains(mx, my) && !waveActive) {
            startNextWave();
        }
    }

    private void handleMapClick(int col, int row) {
        for (Tower t : towers) {
            if (t.getCol() == col && t.getRow() == row) {
                selectedTower = (selectedTower == t) ? null : t;
                return;
            }
        }

        selectedTower = null;

        if (map.isBuildable(col, row) && gold >= selectedBuildType.cost) {
            gold -= selectedBuildType.cost;
            towers.add(new Tower(col, row, selectedBuildType));
        }
    }

    private void startNextWave() {
        wave++;
        waveActive = true;
        enemiesToSpawn = 6 + wave * 3;
        spawnedInWave = 0;
        spawnCooldown = 10;
    }

    private void updateGame() {
        if (gameOver) return;

        if (waveActive && enemiesToSpawn > 0) {
            spawnCooldown--;
            if (spawnCooldown <= 0) {
                Enemy.Type type = Enemy.Type.GOBLIN;
                if (wave >= 3 && spawnedInWave % 4 == 3) {
                    type = Enemy.Type.TANK;
                } else if (wave >= 2 && spawnedInWave % 3 == 2) {
                    type = Enemy.Type.RUNNER;
                }

                enemies.add(new Enemy(type, map.getWaypoints().get(0), wave));
                enemiesToSpawn--;
                spawnedInWave++;
                spawnCooldown = Math.max(18, 42 - wave * 2);
            }
        }

        Iterator<Enemy> enemyIt = enemies.iterator();
        while (enemyIt.hasNext()) {
            Enemy e = enemyIt.next();
            e.update(map.getWaypoints());

            if (!e.isAlive()) {
                if (e.hasReachedBase()) {
                    lives -= e.getBaseDamage();
                    if (lives <= 0) {
                        lives = 0;
                        gameOver = true;
                    }
                } else {
                    gold += e.getReward();
                }
                enemyIt.remove();
            }
        }

        if (waveActive && enemiesToSpawn == 0 && enemies.isEmpty()) {
            waveActive = false;
            gold += 25 + wave * 5;
        }

        for (Tower t : towers) {
            t.update(enemies, projectiles);
        }

        Iterator<Projectile> projIt = projectiles.iterator();
        while (projIt.hasNext()) {
            Projectile p = projIt.next();
            p.update(enemies);
            if (!p.isActive()) {
                if (p.hasExploded()) {
                    explosions.add(new ExplosionEffect(p.getX(), p.getY()));
                }
                projIt.remove();
            }
        }

        explosions.removeIf(ex -> {
            ex.timer--;
            return ex.timer <= 0;
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = getScale();
        int offsetX = getOffsetX(scale);
        int offsetY = getOffsetY(scale);

        AffineTransform originalTransform = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        map.draw(g2);

        // Preview penempatan
        if (hoverCol >= 0 && hoverRow >= 0 && selectedTower == null && !gameOver) {
            boolean occupied = towers.stream().anyMatch(t -> t.getCol() == hoverCol && t.getRow() == hoverRow);
            boolean valid = map.isBuildable(hoverCol, hoverRow) && !occupied && gold >= selectedBuildType.cost;

            int px = hoverCol * GameMap.TILE_SIZE + GameMap.TILE_SIZE / 2;
            int py = hoverRow * GameMap.TILE_SIZE + GameMap.TILE_SIZE / 2;
            int r = (int) selectedBuildType.baseRange;

            Color tint = valid ? selectedBuildType.color : new Color(201, 93, 86);
            float[] dash = {4f, 4f};
            g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 125));
            g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g2.drawOval(px - r, py - r, r * 2, r * 2);

            g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 45));
            g2.fillOval(px - 17, py - 17, 34, 34);
        }

        for (Tower t : towers) {
            t.draw(g2, t == selectedTower);
        }
        for (Enemy e : enemies) {
            e.draw(g2);
        }
        for (Projectile p : projectiles) {
            p.draw(g2);
        }
        for (ExplosionEffect ex : explosions) {
            int radius = (14 - ex.timer) * 4;
            g2.setColor(new Color(224, 108, 101, ex.timer * 14));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval((int) (ex.x - radius), (int) (ex.y - radius), radius * 2, radius * 2);
        }

        if (selectedTower != null && !gameOver) {
            drawTowerInspector(g2, selectedTower);
        }

        drawHud(g2);

        if (gameOver) {
            g2.setColor(new Color(17, 19, 23, 232));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 34));
            g2.drawString("GAME OVER", 295, 295);

            g2.setColor(TEXT_SECONDARY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g2.drawString("Bertahan hingga Wave " + wave + " — Klik untuk mengulang dengan peta baru", 170, 328);
        }

        if (offsetX > 0 || offsetY > 0) {
            g2.setColor(BORDER_MUTED);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(0, 0, WIDTH, HEIGHT);
        }

        g2.setTransform(originalTransform);
    }

    private void drawTowerInspector(Graphics2D g2, Tower tower) {
        int cardW = 192;
        int cardH = 108;

        int cardX = (int) tower.getX() + 28;
        if (cardX + cardW > WIDTH - 10) {
            cardX = (int) tower.getX() - 28 - cardW;
        }

        int cardY = (int) tower.getY() - cardH / 2;
        cardY = Math.max(10, Math.min(MAP_HEIGHT - cardH - 10, cardY));

        g2.setColor(new Color(0, 0, 0, 85));
        g2.fillRoundRect(cardX + 2, cardY + 4, cardW, cardH, 10, 10);

        g2.setColor(new Color(28, 32, 40, 245));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 10, 10);

        g2.setColor(BORDER_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 10, 10);

        g2.setColor(tower.getType().color);
        g2.fillOval(cardX + 12, cardY + 13, 9, 9);

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(tower.getType().name, cardX + 27, cardY + 22);

        g2.setColor(TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString("Lv." + tower.getLevel(), cardX + cardW - 36, cardY + 22);

        g2.setColor(BORDER_MUTED);
        g2.drawLine(cardX + 12, cardY + 30, cardX + cardW - 12, cardY + 30);

        int dmgNow = (int) Math.round(tower.getDamage());
        int dmgNext = (int) Math.round(tower.getNextDamage());
        drawStatRow(g2, cardX, cardY + 46, cardW, "Damage",
                String.valueOf(dmgNow), "→ " + dmgNext);

        int rngNow = (int) Math.round(tower.getRange());
        int rngNext = (int) Math.round(tower.getNextRange());
        drawStatRow(g2, cardX, cardY + 63, cardW, "Range",
                rngNow + "px", "→ " + rngNext);

        String spdNow = String.format(Locale.US, "%.1f/s", tower.getFireRate());
        String spdNext = String.format(Locale.US, "→ %.1f", tower.getNextFireRate());
        drawStatRow(g2, cardX, cardY + 80, cardW, "Speed", spdNow, spdNext);

        g2.setColor(TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString("Efek", cardX + 12, cardY + 97);

        g2.setColor(tower.getType().color);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(tower.getType().effectDesc, cardX + 64, cardY + 97);
    }

    private void drawStatRow(Graphics2D g2, int cardX, int y, int cardW, String label, String current, String next) {
        g2.setColor(TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(label, cardX + 12, y);

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(current, cardX + 64, y);

        g2.setColor(ACCENT_UPGRADE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString(next, cardX + cardW - 52, y);
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(HUD_BG);
        g2.fillRect(0, MAP_HEIGHT, WIDTH, HUD_HEIGHT);

        g2.setColor(BORDER_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, MAP_HEIGHT, WIDTH, MAP_HEIGHT);

        drawMetric(g2, "GOLD", "$" + gold, 24, MAP_HEIGHT + 27);
        drawMetric(g2, "LIVES", String.valueOf(lives), 132, MAP_HEIGHT + 27);
        drawMetric(g2, "WAVE", String.valueOf(wave), 224, MAP_HEIGHT + 27);

        g2.setColor(TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String hint;
        if (selectedTower != null) {
            hint = String.format(Locale.US, "%s Lv.%d • DMG %d • RNG %d",
                    selectedTower.getType().name,
                    selectedTower.getLevel(),
                    Math.round(selectedTower.getDamage()),
                    Math.round(selectedTower.getRange()));
        } else {
            hint = "Klik petak kosong untuk bangun • Klik tower untuk detail";
        }
        g2.drawString(hint, 315, MAP_HEIGHT + 27);

        drawMiniPillButton(g2, btnRerollMap, "Acak Peta", wave == 0 && !waveActive);
        drawMiniPillButton(g2, btnFullScreen, Main.isFullScreen() ? "Window" : "Full [F11]", true);

        drawTowerButton(g2, btnArcher, Tower.Type.ARCHER, selectedTower == null && selectedBuildType == Tower.Type.ARCHER);
        drawTowerButton(g2, btnIce, Tower.Type.ICE, selectedTower == null && selectedBuildType == Tower.Type.ICE);
        drawTowerButton(g2, btnCannon, Tower.Type.CANNON, selectedTower == null && selectedBuildType == Tower.Type.CANNON);

        boolean canUpgrade = selectedTower != null && gold >= selectedTower.getUpgradeCost();
        drawActionButton(g2, btnUpgrade,
                "Upgrade",
                selectedTower != null ? "$" + selectedTower.getUpgradeCost() : "—",
                canUpgrade, false);

        drawActionButton(g2, btnSell,
                "Sell",
                selectedTower != null ? "+$" + selectedTower.getSellValue() : "—",
                selectedTower != null, false);

        drawActionButton(g2, btnWave,
                waveActive ? "Berlangsung" : "Mulai Wave",
                waveActive ? "Sisa " + (enemiesToSpawn + enemies.size()) : "Wave " + (wave + 1),
                !waveActive, !waveActive);
    }

    private void drawMiniPillButton(Graphics2D g2, Rectangle rect, String label, boolean enabled) {
        g2.setColor(enabled ? CARD_BG : BTN_DISABLED_BG);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 6, 6);

        g2.setColor(BORDER_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 6, 6);

        g2.setColor(enabled ? TEXT_PRIMARY : TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        int textW = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, rect.x + (rect.width - textW) / 2, rect.y + 16);
    }

    private void drawMetric(Graphics2D g2, String label, String value, int x, int y) {
        g2.setColor(TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(label, x, y);

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(value, x + 40, y);
    }

    private void drawTowerButton(Graphics2D g2, Rectangle rect, Tower.Type type, boolean active) {
        g2.setColor(active ? new Color(36, 42, 54) : CARD_BG);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        g2.setColor(active ? type.color : BORDER_MUTED);
        g2.setStroke(new BasicStroke(active ? 1.5f : 1f));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        g2.setColor(type.color);
        g2.fillOval(rect.x + 12, rect.y + 18, 12, 12);

        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(type.name, rect.x + 32, rect.y + 22);

        g2.setColor(gold >= type.cost ? TEXT_SECONDARY : new Color(201, 93, 86));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString("$" + type.cost, rect.x + 32, rect.y + 37);
    }

    private void drawActionButton(Graphics2D g2, Rectangle rect, String title, String sub, boolean enabled, boolean primary) {
        if (primary && enabled) {
            g2.setColor(TEXT_PRIMARY);
        } else {
            g2.setColor(enabled ? CARD_BG : BTN_DISABLED_BG);
        }
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        g2.setColor(primary && enabled ? TEXT_PRIMARY : BORDER_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        g2.setColor(primary && enabled ? OUTER_BG : (enabled ? TEXT_PRIMARY : TEXT_SECONDARY));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(title, rect.x + 14, rect.y + 22);

        g2.setColor(primary && enabled ? new Color(65, 72, 84) : TEXT_SECONDARY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(sub, rect.x + 14, rect.y + 37);
    }

    private static class ExplosionEffect {
        double x, y;
        int timer = 13;

        ExplosionEffect(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}