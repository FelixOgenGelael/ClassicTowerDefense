package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class Projectile {
    private double x;
    private double y;
    private final double speed = 9.5;
    private final Enemy target;
    private final double damage;
    private final Tower.Type sourceType;

    private double lastTargetX;
    private double lastTargetY;
    private boolean active = true;
    private boolean exploded = false;

    public Projectile(double x, double y, Enemy target, double damage, Tower.Type sourceType) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
        this.sourceType = sourceType;
        this.lastTargetX = target.getX();
        this.lastTargetY = target.getY();
    }

    public void update(List<Enemy> enemies) {
        if (!active) return;

        if (target != null && target.isAlive()) {
            lastTargetX = target.getX();
            lastTargetY = target.getY();
        }

        double dx = lastTargetX - x;
        double dy = lastTargetY - y;
        double dist = Math.hypot(dx, dy);

        if (dist <= speed) {
            x = lastTargetX;
            y = lastTargetY;
            onHit(enemies);
        } else {
            x += (dx / dist) * speed;
            y += (dy / dist) * speed;
        }
    }

    private void onHit(List<Enemy> enemies) {
        if (sourceType == Tower.Type.CANNON) {
            exploded = true;
            double splashRadius = 65.0;
            for (Enemy e : enemies) {
                if (e.isAlive() && Math.hypot(e.getX() - x, e.getY() - y) <= splashRadius) {
                    e.takeDamage(damage);
                }
            }
        } else if (target != null && target.isAlive()) {
            target.takeDamage(damage);
            if (sourceType == Tower.Type.ICE) {
                target.applySlow(120);
            }
        }
        active = false;
    }

    public void draw(Graphics2D g2) {
        g2.setColor(sourceType.color);
        int size = sourceType == Tower.Type.CANNON ? 7 : 5;
        g2.fillOval((int) (x - size / 2.0), (int) (y - size / 2.0), size, size);
    }

    public boolean isActive() { return active; }
    public boolean hasExploded() { return exploded; }
    public double getX() { return x; }
    public double getY() { return y; }
}