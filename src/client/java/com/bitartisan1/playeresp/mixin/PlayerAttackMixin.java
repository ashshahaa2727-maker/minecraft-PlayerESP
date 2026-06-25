package com.bitartisan1.playeresp.mixin;

import com.bitartisan1.playeresp.TargetESPManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class PlayerAttackMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity hitPlayer) {
            // Spawn hit particles at the target's position
            Vec3d pos = hitPlayer.getPos().add(0, hitPlayer.getHeight() / 2.0, 0);
            TargetESPManager.spawnHitParticles(pos);
        }
    }
}
