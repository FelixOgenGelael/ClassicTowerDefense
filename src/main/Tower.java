package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

public class Tower {
    public enum Type {
        ARCHER("Archer", 50, 130, 24, 26, new Color(91, 155, 213), "Single target"),
        ICE("Ice", 75, 115, 10, 38, new Color(78, 205, 196), "Slow 50% (2.0s)"),
        CANNON("Cannon", 100, 145, 55, 65, new Color(224, 108, 101), "Splash 65px");

        public final String name;
        public final int cost;
        public final double baseRange;
        public final double baseDamage;
        public final int cooldown;
        public final Color color;
        public final String effectDesc;

        Type(String name, int cost, double baseRange, double baseDamage, int cooldown, Color color, String effectDesc) {
            this.name = name;
            this.cost = cost;
            this.baseRange = baseRange;
            this.baseDamage = baseDamage;
            this.cooldown = cooldown;
            this.color = color;
            this.effectDesc = effectDesc;
        }
    }

    private final int col;
    private final int row;
    private final double x;
    private final double y;
    private final Type type;

    private int level = 1;
    private int totalInvested;
    private int cooldownTimer = 0;
    private double angle = 0;

    public Tower(int col, int row, Type type) {
        this.col = col;
        this.row = row;
        this.x = col * GameMap.TILE_SIZE + GameMap.TILE_SIZE / 2.0;
        this.y = row * GameMap.TILE_SIZE + GameMap.TILE_SIZE / 2.0;
        this.type = type;
        this.totalInvested = type.cost;
    }

    public double getRange() {
        return type.baseRange + (level - 1) * 18.0;
    }

    public double getNextRange() {
        return type.baseRange + level * 18.0;
    }

    public double getDamage() {
        return type.baseDamage * (1.0 + (level - 1) * 0.45);
    }

    public double getNextDamage() {
        return type.baseDamage * (1.0 + level * 0.45);
    }

    public int getEffectiveCooldown() {
        return Math.max(10, type.cooldown - (level - 1) * 3);
    }

    public double getFireRate() {
        return 60.0 / getEffectiveCooldown();
    }

    public double getNextFireRate() {
        int nextCd = Math.max(10, type.cooldown - level * 3);
        return 60.0 / nextCd;
    }

    public int getUpgradeCost() {
        return (int) (type.cost * 0.8 * level);
    }

    public int getSellValue() {
        return (int) (totalInvested * 0.7);
    }

    public void upgrade() {
        totalInvested += getUpgradeCost();
        level++;
        cooldownTimer = Math.min(cooldownTimer, getEffectiveCooldown());
    }

    public void update(List<Enemy> enemies, List<Projectile> projectiles) {
        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        Enemy target = findTarget(enemies);
        if (target != null) {
            angle = Math.atan2(target.getY() - y, target.getX() - x);

            if (cooldownTimer <= 0) {
                projectiles.add(new Projectile(x, y, target, getDamage(), type));
                cooldownTimer = getEffectiveCooldown();
            }
        }
    }

    private Enemy findTarget(List<Enemy> enemies) {
        Enemy bestTarget = null;
        int highestWaypoint = -1;

        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            double dist = Math.hypot(e.getX() - x, e.getY() - y);
            if (dist <= getRange()) {
                if (e.getWaypointIndex() > highestWaypoint) {
                    highestWaypoint = e.getWaypointIndex();
                    bestTarget = e;
                }
            }
        }
        return bestTarget;
    }

    public void draw(Graphics2D g2, boolean selected) {
        int tileX = col * GameMap.TILE_SIZE;
        int tileY = row * GameMap.TILE_SIZE;

        if (selected) {
            int r = (int) getRange();
            g2.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), 24));
            g2.fillOval((int) (x - r), (int) (y - r), r * 2, r * 2);

            float[] dash = {5f, 5f};
            g2.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), 140));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g2.drawOval((int) (x - r), (int) (y - r), r * 2, r * 2);
        }

        // Dudukan dark slate minimalis
        g2.setColor(new Color(35, 40, 50));
        g2.fillOval(tileX + 8, tileY + 8, 34, 34);
        g2.setColor(selected ? type.color : new Color(58, 66, 82));
        g2.setStroke(new BasicStroke(selected ? 1.8f : 1.2f));
        g2.drawOval(tileX + 8, tileY + 8, 34, 34);

        // Inti turret
        g2.setColor(type.color);
        g2.fillOval(tileX + 16, tileY + 16, 18, 18);

        // Laras meriam
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);
        g2.setColor(type.color);
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(0, 0, 16, 0);
        g2.setTransform(old);

        // Titik level
        g2.setColor(new Color(160, 168, 182));
        int count = Math.min(level, 5);
        int startX = (int) (x - ((count - 1) * 5) / 2.0);
        for (int i = 0; i < count; i++) {
            g2.fillOval(startX + i * 5 - 1, tileY + 44, 3, 3);
        }
    }

    public int getCol() { return col; }
    public int getRow() { return row; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getLevel() { return level; }
    public Type getType() { return type; }
}