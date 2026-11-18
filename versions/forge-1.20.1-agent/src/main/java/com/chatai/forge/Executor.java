package com.chatai.forge;

import com.chatai.core.Intent;
import com.chatai.core.Messages;
import com.chatai.core.Structures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

/** Runs a validated Intent on the SERVER thread. Never executes anything except these fixed actions. */
public final class Executor {
    private static final Random RNG = new Random();
    private static final int MAX_MOBS = 100;

    private Executor() {}

    /** @return short detail text for the success message. */
    public static String run(ServerPlayer requester, ServerPlayer subject, Intent i, String lang) throws ActionException {
        int budget = ChatAIConfig.MAX_ACTIONS.get();
        String detail;
        switch (i.type) {
            case GIVE: detail = give(subject, i, budget, lang); break;
            case TIME: setTime(subject.serverLevel(), i.timeOfDay); detail = i.label; break;
            case WEATHER: setWeather(subject.serverLevel(), i.weather); detail = i.label; break;
            case EFFECT: detail = effect(subject, i); break;
            case SPAWN: detail = spawn(subject, i, Math.min(budget, MAX_MOBS), lang); break;
            case BUILD: detail = build(requester, subject, i, lang); break;
            case TELEPORT: detail = teleport(requester, subject, i); break;
            case FIREWORK: detail = firework(subject, i); break;
            case HEAL: subject.setHealth(subject.getMaxHealth()); subject.getFoodData().setFoodLevel(20); detail = ""; break;
            default: throw new ActionException(Messages.notUnderstood(lang));
        }
        if (ChatAIConfig.SOUNDS.get()) {
            subject.level().playSound(null, subject.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        return detail;
    }

    private static ResourceLocation mc(String id) { return new ResourceLocation("minecraft", id); }

    private static String give(ServerPlayer p, Intent i, int budget, String lang) throws ActionException {
        Item item = ForgeRegistries.ITEMS.getValue(mc(i.id));
        if (item == null || item == Items.AIR) throw new ActionException(Messages.unknownItem(lang));
        int left = Math.min(i.count, Math.min(budget, 2304));
        int total = left;
        while (left > 0) {
            int n = Math.min(left, Math.max(1, item.getMaxStackSize()));
            ItemStack st = new ItemStack(item, n);
            if (!p.getInventory().add(st) && !st.isEmpty()) p.drop(st, false);
            left -= n;
        }
        return total + " x " + i.id;
    }

    private static void setTime(ServerLevel level, long ticks) {
        long now = level.getDayTime();
        long base = now - Math.floorMod(now, 24000L);
        long target = base + ticks;
        if (target <= now) target += 24000L;
        level.setDayTime(target);
    }

    private static void setWeather(ServerLevel level, Intent.Weather w) {
        switch (w) {
            case CLEAR: level.setWeatherParameters(6000, 0, false, false); break;
            case RAIN: level.setWeatherParameters(0, 6000, true, false); break;
            case THUNDER: level.setWeatherParameters(0, 6000, true, true); break;
        }
    }

    private static String effect(ServerPlayer p, Intent i) throws ActionException {
        MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(mc(i.id));
        if (eff == null) throw new ActionException("Unknown effect: " + i.id);
        p.addEffect(new MobEffectInstance(eff, i.durationSeconds * 20, i.amplifier));
        return i.id + " " + i.durationSeconds + "s";
    }

    private static String spawn(ServerPlayer p, Intent i, int cap, String lang) throws ActionException {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(mc(i.id));
        if (type == null) throw new ActionException("Unknown creature: " + i.id);
        ServerLevel level = p.serverLevel();
        int n = Math.min(i.count, cap), made = 0;
        for (int k = 0; k < n; k++) {
            int x = p.getBlockX() + RNG.nextInt(11) - 5, z = p.getBlockZ() + RNG.nextInt(11) - 5;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isInWorldBounds(pos)) continue;
            Entity e = type.create(level);
            if (e == null) continue;
            e.moveTo(x + 0.5, y, z + 0.5, RNG.nextFloat() * 360f, 0f);
            if (e instanceof Mob mob) mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null, null);
            if (level.addFreshEntity(e)) made++;
        }
        if (made == 0) throw new ActionException(Messages.noSpace(lang));
        return made + " x " + i.id;
    }

    private static String teleport(ServerPlayer requester, ServerPlayer subject, Intent i) {
        // subject is the destination, unless id == to_requester (then subject is moved to the requester)
        ServerPlayer mover = "to_requester".equals(i.id) ? subject : requester;
        ServerPlayer dest = "to_requester".equals(i.id) ? requester : subject;
        mover.teleportTo(dest.serverLevel(), dest.getX(), dest.getY(), dest.getZ(), dest.getYRot(), dest.getXRot());
        return "";
    }

    private static String firework(ServerPlayer p, Intent i) {
        ServerLevel level = p.serverLevel();
        int n = Math.min(Math.max(i.count, 1), 20);
        int[] palette = {0x8A2BE2, 0xFF69B4, 0xFFD700, 0x00BFFF, 0x7CFC00, 0xFF4500};
        for (int k = 0; k < n; k++) {
            ItemStack s = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fw = s.getOrCreateTagElement("Fireworks");
            fw.putByte("Flight", (byte) (1 + RNG.nextInt(2)));
            ListTag ex = new ListTag();
            CompoundTag e = new CompoundTag();
            e.putByte("Type", (byte) RNG.nextInt(5));
            e.putIntArray("Colors", new int[]{palette[RNG.nextInt(palette.length)], palette[RNG.nextInt(palette.length)]});
            ex.add(e);
            fw.put("Explosions", ex);
            double x = p.getX() + RNG.nextInt(9) - 4, z = p.getZ() + RNG.nextInt(9) - 4;
            level.addFreshEntity(new FireworkRocketEntity(level, x, p.getY(), z, s));
        }
        return "";
    }

    private static Block material(String key) {
        switch (key) {
            case "PLANKS": return Blocks.OAK_PLANKS;
            case "LOG": return Blocks.OAK_LOG;
            case "COBBLE": return Blocks.COBBLESTONE;
            case "STONE_BRICK": return Blocks.STONE_BRICKS;
            case "GLASS": return Blocks.GLASS;
            case "TORCH": return Blocks.TORCH;
            case "QUARTZ": return Blocks.QUARTZ_BLOCK;
            case "SANDSTONE": return Blocks.SANDSTONE;
            default: return Blocks.AIR;
        }
    }

    private static String build(ServerPlayer requester, ServerPlayer subject, Intent i, String lang) throws ActionException {
        ServerLevel level = subject.serverLevel();
        Direction f = subject.getDirection();
        BlockPos base = subject.blockPosition().relative(f, 5);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
        BlockPos origin = new BlockPos(base.getX(), y, base.getZ());
        List<Structures.Placement> plan = Structures.build(i.id);
        if (plan.isEmpty()) throw new ActionException(Messages.notUnderstood(lang));
        if (!requester.mayBuild()) throw new ActionException(Messages.noPermission(lang));
        // Validate EVERYTHING first (world bounds, spawn protection) so a build is never half-placed.
        for (Structures.Placement p : plan) {
            BlockPos bp = origin.offset(p.dx, p.dy, p.dz);
            if (!level.isInWorldBounds(bp) || !level.mayInteract(requester, bp)) throw new ActionException(Messages.noSpace(lang));
        }
        for (Structures.Placement p : plan) {
            level.setBlock(origin.offset(p.dx, p.dy, p.dz), material(p.material).defaultBlockState(), 3);
        }
        return plan.size() + " blocks";
    }
}
