package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

public class Enemy {
    public enum Type {
        GOBLIN(80, 1.6, 12, 1, new Color(91, 185, 116), 20),
        RUNNER(50, 2.7, 15, 1, new Color(229, 181, 79), 16),
        TANK(240, 0.95, 30, 3, new Color(217, 91, 91), 28);

        public final double baseHp;
        public final double speed;
        public final int reward;
        public final int baseDamage;
        public final Color color;
        public final int size;

        Type(double baseHp, double speed, int reward, int baseDamage, Color color, int size) {
            this.baseHp = baseHp;
            this.speed = speed;
            this.reward = reward;
            this.baseDamage = baseDamage;
            this.color = color;
            this.size = size;
        }
    }

    private double x;
    private double y;
    private final double maxHp;
    private double hp;
    private final double baseSpeed;
    private final int reward;
    private final int baseDamage;
    private final Type type;

    private int waypointIndex = 1;
    private boolean alive = true;
    private boolean reachedBase = false;
    private int slowTimer = 0;

    public Enemy(Type type, Point spawnPoint, int wave) {
        this.type = type;
        this.x = spawnPoint.x;
        this.y = spawnPoint.y;

        double scale = 1.0 + (wave - 1) * 0.28;
        this.maxHp = type.baseHp * scale;
        this.hp = this.maxHp;
        this.baseSpeed = type.speed;
        this.reward = type.reward;
        this.baseDamage = type.baseDamage;
    }

    public void update(List<Point> waypoints) {
        if (!alive) return;

        if (slowTimer > 0) {
            slowTimer--;
        }

        double currentSpeed = slowTimer > 0 ? baseSpeed * 0.5 : baseSpeed;
        Point target = waypoints.get(waypointIndex);

        double dx = target.x - x;
        double dy = target.y - y;
        double dist = Math.hypot(dx, dy);

        if (dist <= currentSpeed) {
            x = target.x;
            y = target.y;
            waypointIndex++;
            if (waypointIndex >= waypoints.size()) {
                reachedBase = true;
                alive = false;
            }
        } else {
            x += (dx / dist) * currentSpeed;
            y += (dy / dist) * currentSpeed;
        }
    }

    public void takeDamage(double damage) {
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }

    public void applySlow(int frames) {
        slowTimer = Math.max(slowTimer, frames);
    }

    public void draw(Graphics2D g2) {
        int s = type.size;
        int drawX = (int) (x - s / 2.0);
        int drawY = (int) (y - s / 2.0);

        g2.setColor(slowTimer > 0 ? new Color(124, 198, 254) : type.color);
        g2.fillOval(drawX, drawY, s, s);

        if (hp < maxHp) {
            int barW = 22;
            int barH = 3;
            int barX = (int) (x - barW / 2.0);
            int barY = drawY - 7;

            g2.setColor(new Color(48, 54, 66));
            g2.fillRoundRect(barX, barY, barW, barH, 2, 2);

            int hpW = (int) ((hp / maxHp) * barW);
            g2.setColor(type.color);
            g2.fillRoundRect(barX, barY, Math.max(0, hpW), barH, 2, 2);
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getReward() { return reward; }
    public int getBaseDamage() { return baseDamage; }
    public boolean isAlive() { return alive; }
    public boolean hasReachedBase() { return reachedBase; }
    public int getWaypointIndex() { return waypointIndex; }
}