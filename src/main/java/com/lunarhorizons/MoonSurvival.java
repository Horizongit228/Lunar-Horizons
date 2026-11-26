package com.lunarhorizons;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class MoonSurvival {
    private static final Identifier GRAVITY = LunarHorizons.id("moon_gravity");
    private static final Identifier FALL = LunarHorizons.id("moon_fall");
    private static void modifier(EntityAttributeInstance attribute, Identifier id, double value, boolean enabled) {
        if (attribute == null) return;
        if (enabled && !attribute.hasModifier(id)) attribute.addTemporaryModifier(new EntityAttributeModifier(id, value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (!enabled && attribute.hasModifier(id)) attribute.removeModifier(id);
    }
    static int tankSlot(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(LunarHorizons.OXYGEN) && stack.getDamage() < stack.getMaxDamage()) return i;
        }
        return -1;
    }
    static void tick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            boolean moon = p.getWorld().getRegistryKey().equals(LunarHorizons.MOON);
            modifier(p.getAttributeInstance(EntityAttributes.GENERIC_GRAVITY), GRAVITY, -0.75, moon);
            modifier(p.getAttributeInstance(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER), FALL, -1.0, moon);
            if (!moon || !p.isAlive() || p.isCreative() || p.isSpectator() || server.getTicks() % 20 != 0 || p.getVehicle() instanceof RocketEntity) continue;
            int slot = tankSlot(p);
            if (slot >= 0 && LunarHorizons.hasHelmet(p)) {
                ItemStack tank = p.getInventory().getStack(slot);
                int seconds = tank.getMaxDamage() - tank.getDamage() - 1;
                if (seconds == 0) p.getInventory().setStack(slot, ItemStack.EMPTY);
                else tank.setDamage(tank.getDamage() + 1);
                p.getInventory().markDirty();
                p.sendMessage(Text.translatable("message.lunar_horizons.oxygen", seconds / 60, String.format(java.util.Locale.ROOT, "%02d", seconds % 60)), true);
            } else {
                p.damage(p.getDamageSources().drown(), 2.0F);
                p.sendMessage(Text.translatable("message.lunar_horizons.no_oxygen"), true);
            }
        }
    }
}
