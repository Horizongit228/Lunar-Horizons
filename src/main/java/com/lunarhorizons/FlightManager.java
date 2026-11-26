package com.lunarhorizons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

final class FlightManager {
    private record Site(RegistryKey<World> world, BlockPos pos) {}
    private record Flight(Site site, int started) {}
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    private static final Set<Site> BUSY = new HashSet<>();
    private static final int DURATION = 100;
    static void clear() { FLIGHTS.clear(); BUSY.clear(); }
    static boolean isLaunching(ServerPlayerEntity p) { return FLIGHTS.containsKey(p.getUuid()); }
    private static void message(ServerPlayerEntity p, String key, Object... args) { p.sendMessage(Text.translatable("message.lunar_horizons." + key, args), true); }
    private static int fuelSlot(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++) if (p.getInventory().getStack(i).isOf(LunarHorizons.FUEL)) return i;
        return -1;
    }
    private static boolean pad(ServerWorld w, BlockPos p) { return w.getBlockState(p.down()).isOf(LunarHorizons.LAUNCH_PAD); }
    static void start(ServerPlayerEntity p, ServerWorld w, BlockPos pos) {
        if (isLaunching(p)) return;
        Site site = new Site(w.getRegistryKey(), pos.toImmutable());
        if (BUSY.contains(site)) { message(p, "busy"); return; }
        if (!w.getRegistryKey().equals(World.OVERWORLD) && !w.getRegistryKey().equals(LunarHorizons.MOON)) { message(p, "wrong_world"); return; }
        if (!pad(w, pos)) { message(p, "need_pad"); return; }
        if (!w.getBlockState(pos.up()).isAir() || !w.getBlockState(pos.up(2)).isAir()) { message(p, "blocked"); return; }
        if (!p.isCreative() && fuelSlot(p) < 0) { message(p, "need_fuel"); return; }
        if (w.getRegistryKey().equals(World.OVERWORLD) && !p.isCreative() && (!LunarHorizons.hasHelmet(p) || MoonSurvival.tankSlot(p) < 0)) { message(p, "need_suit"); return; }
        if (p.getServer() == null) return;
        FLIGHTS.put(p.getUuid(), new Flight(site, p.getServer().getTicks()));
        BUSY.add(site);
        message(p, "countdown", 5);
    }
    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Flight>> it = FLIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Flight f = entry.getValue();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
            ServerWorld w = server.getWorld(f.site.world);
            BlockPos pos = f.site.pos;
            int elapsed = server.getTicks() - f.started;
            boolean valid = p != null && p.isAlive() && !p.isSpectator() && w != null && p.getWorld() == w && p.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 25 && w.getBlockState(pos).isOf(LunarHorizons.ROCKET) && pad(w, pos) && w.getBlockState(pos.up()).isAir() && w.getBlockState(pos.up(2)).isAir();
            if (!valid || elapsed < 0 || elapsed >= DURATION) {
                it.remove(); BUSY.remove(f.site);
                if (!valid || elapsed < 0) { if (p != null) message(p, "cancelled"); }
                else finish(server, p, w, pos);
                continue;
            }
            if (elapsed % 20 == 0) message(p, "countdown", (DURATION - elapsed) / 20);
            if (elapsed % 5 == 0) {
                w.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX()+0.5, pos.getY()+0.15, pos.getZ()+0.5, 8, .5, .1, .5, .02);
                if (elapsed > 60) w.spawnParticles(ParticleTypes.FLAME, pos.getX()+0.5, pos.getY()+0.1, pos.getZ()+0.5, 12, .4, .1, .4, .03);
            }
        }
    }
    private static void finish(MinecraftServer server, ServerPlayerEntity p, ServerWorld source, BlockPos origin) {
        int fuel = fuelSlot(p);
        boolean toMoon = source.getRegistryKey().equals(World.OVERWORLD);
        if (!p.isCreative() && (fuel < 0 || (toMoon && (!LunarHorizons.hasHelmet(p) || MoonSurvival.tankSlot(p) < 0)))) { message(p, "cancelled"); return; }
        ServerWorld target = server.getWorld(toMoon ? LunarHorizons.MOON : World.OVERWORLD);
        if (target == null) { message(p, "missing_world"); return; }
        BlockPos landing = findLanding(target, origin);
        if (landing == null) { message(p, "no_landing"); return; }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) target.setBlockState(landing.add(x, -1, z), LunarHorizons.LAUNCH_PAD.getDefaultState());
        target.setBlockState(landing, LunarHorizons.ROCKET.getDefaultState());
        source.removeBlock(origin, false);
        if (!p.isCreative()) { ItemStack stack = p.getInventory().getStack(fuel); stack.decrement(1); p.getInventory().markDirty(); }
        p.teleportTo(new TeleportTarget(target, Vec3d.ofBottomCenter(landing.east()), Vec3d.ZERO, p.getYaw(), 0, TeleportTarget.NO_OP));
        message(p, toMoon ? "arrived_moon" : "arrived_earth");
    }
    private static BlockPos findLanding(ServerWorld w, BlockPos origin) {
        for (int ring = 0; ring <= 3; ring++) for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
            int x = origin.getX() + dx*4, z = origin.getZ() + dz*4;
            int y = w.getBottomY();
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) y = Math.max(y, w.getTopY(Heightmap.Type.MOTION_BLOCKING, x+a, z+b));
            BlockPos pad = new BlockPos(x, y, z);
            if (!w.isInBuildLimit(pad.up(3))) continue;
            boolean clear = true;
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) for (int h = 0; h <= 3; h++) {
                BlockPos test = pad.add(a,h,b);
                if (!w.getWorldBorder().contains(test) || !w.getBlockState(test).isAir()) clear = false;
            }
            if (clear) return pad.up();
        }
        return null;
    }
}
