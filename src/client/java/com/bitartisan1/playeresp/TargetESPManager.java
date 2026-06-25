package com.bitartisan1.playeresp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TargetESPManager {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    // Currently locked target
    private static PlayerEntity currentTarget = null;

    // Orbit particles around target
    private static final List<OrbitParticle> orbitParticles = new ArrayList<>();
    private static final int ORBIT_PARTICLE_COUNT = 18;

    // Hit particles (burst on attack)
    private static final List<HitParticle> hitParticles = new ArrayList<>();

    // Tick counter for animation
    private static long tickCount = 0;

    // ===================== ORBIT PARTICLES =====================

    public static class OrbitParticle {
        // Angle around target (radians)
        public double angle;
        // Vertical offset oscillation phase
        public double vertPhase;
        // Radius of orbit
        public double radius;
        // Speed of orbit
        public double speed;
        // Height offset
        public double heightOffset;
        // Chaos: random wobble offsets that change over time
        public double wobbleX;
        public double wobbleZ;
        public double wobbleSpeed;
        // Color (r,g,b)
        public float r, g, b;
        // Size
        public float size;
        // Current rendered position
        public Vec3d pos = Vec3d.ZERO;

        public OrbitParticle(int index, int total) {
            // Spread evenly, then add randomness
            angle = (2.0 * Math.PI * index / total) + random.nextDouble() * 0.5;
            vertPhase = random.nextDouble() * Math.PI * 2;
            radius = 0.6 + random.nextDouble() * 0.6;
            speed = 0.015 + random.nextDouble() * 0.03;
            heightOffset = 0.5 + random.nextDouble() * 1.5;
            wobbleX = random.nextDouble() * Math.PI * 2;
            wobbleZ = random.nextDouble() * Math.PI * 2;
            wobbleSpeed = 0.02 + random.nextDouble() * 0.03;
            size = 0.04f + random.nextFloat() * 0.04f;

            // Random warm color: white, yellow, orange
            float colorChoice = random.nextFloat();
            if (colorChoice < 0.33f) {
                // White/silver
                r = 0.9f; g = 0.9f; b = 0.9f;
            } else if (colorChoice < 0.66f) {
                // Yellow
                r = 1.0f; g = 0.9f; b = 0.2f;
            } else {
                // Orange
                r = 1.0f; g = 0.5f; b = 0.1f;
            }
        }

        public void tick(Vec3d targetCenter) {
            angle += speed;
            wobbleX += wobbleSpeed;
            wobbleZ += wobbleSpeed * 0.7;

            double wobX = Math.sin(wobbleX) * 0.3;
            double wobZ = Math.cos(wobbleZ) * 0.3;
            double vertOscil = Math.sin(tickCount * 0.05 + vertPhase) * 0.4;

            double px = targetCenter.x + Math.cos(angle) * radius + wobX;
            double py = targetCenter.y + heightOffset + vertOscil;
            double pz = targetCenter.z + Math.sin(angle) * radius + wobZ;

            pos = new Vec3d(px, py, pz);
        }
    }

    // ===================== HIT PARTICLES =====================

    public static class HitParticle {
        public Vec3d pos;
        public Vec3d velocity;
        public float r, g, b;
        public float size;
        public int life;
        public int maxLife;
        public float alpha;

        public HitParticle(Vec3d origin) {
            // Random outward velocity
            double vx = (random.nextDouble() - 0.5) * 0.25;
            double vy = random.nextDouble() * 0.2 + 0.05;
            double vz = (random.nextDouble() - 0.5) * 0.25;
            velocity = new Vec3d(vx, vy, vz);
            pos = origin;
            size = 0.04f + random.nextFloat() * 0.06f;
            maxLife = 8 + random.nextInt(10);
            life = maxLife;
            alpha = 1.0f;

            // Red / orange gradient
            float t = random.nextFloat();
            r = 1.0f;
            g = t * 0.45f;  // 0 = red, 0.45 = orange-ish
            b = 0.0f;
        }

        public void tick() {
            pos = pos.add(velocity);
            velocity = new Vec3d(velocity.x * 0.85, velocity.y - 0.015, velocity.z * 0.85);
            life--;
            alpha = (float) life / maxLife;
        }

        public boolean isDead() {
            return life <= 0;
        }
    }

    // ===================== PUBLIC API =====================

    public static void tick() {
        tickCount++;

        if (mc.world == null || mc.player == null) {
            currentTarget = null;
            orbitParticles.clear();
            return;
        }

        // Find closest whitelisted player in crosshair / just nearest
        updateTarget();

        // Tick orbit particles
        if (currentTarget != null) {
            if (orbitParticles.isEmpty()) {
                initOrbitParticles();
            }
            Vec3d center = currentTarget.getPos().add(0, currentTarget.getHeight() / 2.0, 0);
            for (OrbitParticle p : orbitParticles) {
                p.tick(center);
            }
        } else {
            orbitParticles.clear();
        }

        // Tick hit particles
        Iterator<HitParticle> it = hitParticles.iterator();
        while (it.hasNext()) {
            HitParticle p = it.next();
            p.tick();
            if (p.isDead()) it.remove();
        }
    }

    private static void updateTarget() {
        if (mc.targetedEntity instanceof PlayerEntity p && p != mc.player) {
            if (PlayerESPCommand.isWhitelisted(p.getName().getString())) {
                if (currentTarget != p) {
                    currentTarget = p;
                    orbitParticles.clear(); // reset on new target
                }
                return;
            }
        }
        // No target on crosshair — find nearest whitelisted player within 20 blocks
        PlayerEntity nearest = null;
        double nearestDist = 20.0;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!PlayerESPCommand.isWhitelisted(player.getName().getString())) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        if (nearest != currentTarget) {
            currentTarget = nearest;
            orbitParticles.clear();
        }
    }

    private static void initOrbitParticles() {
        orbitParticles.clear();
        for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
            orbitParticles.add(new OrbitParticle(i, ORBIT_PARTICLE_COUNT));
        }
    }

    public static void spawnHitParticles(Vec3d pos) {
        // Spawn 20-30 hit particles
        int count = 20 + random.nextInt(12);
        for (int i = 0; i < count; i++) {
            hitParticles.add(new HitParticle(pos));
        }
    }

    public static PlayerEntity getCurrentTarget() {
        return currentTarget;
    }

    public static List<OrbitParticle> getOrbitParticles() {
        return orbitParticles;
    }

    public static List<HitParticle> getHitParticles() {
        return hitParticles;
    }
}
