package com.lunarhorizons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.registry.tag.DamageTypeTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.registry.entry.RegistryEntry;
import java.util.List;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class LunarHorizons implements ModInitializer {
    public static final String ID = "lunar_horizons";
    public static final RegistryKey<World> MOON = RegistryKey.of(RegistryKeys.WORLD, id("moon"));
    public static final Block REGOLITH = block("regolith", AbstractBlock.Settings.copy(Blocks.DIRT));
    public static final Block MOON_ROCK = block("moon_rock", AbstractBlock.Settings.copy(Blocks.STONE));
    public static final Block CRYSTAL_ORE = block("crystal_ore", AbstractBlock.Settings.copy(Blocks.IRON_ORE).luminance(s -> 5));
    public static final Block MOON_BRICKS = block("moon_bricks", AbstractBlock.Settings.copy(Blocks.STONE_BRICKS));
    public static final Block LAUNCH_PAD = block("launch_pad", AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    // Retain the old block ID for existing saves. Its item now deploys a real vehicle.
    public static final EntityType<RocketEntity> ROCKET_ENTITY = Registry.register(Registries.ENTITY_TYPE, id("rocket"),
            EntityType.Builder.<RocketEntity>create(RocketEntity::new, SpawnGroup.MISC)
                    .dimensions(3.0F, 8.0F).maxTrackingRange(16).trackingTickInterval(1)
                    .makeFireImmune().build(ID + ":rocket"));
    public static final Block ROCKET = Registry.register(Registries.BLOCK, id("rocket"),
            new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque()));
    public static final Item ROCKET_ITEM = item("rocket", new RocketItem(ROCKET, new Item.Settings().maxCount(1)));
    public static final Item CRYSTAL = item("lunar_crystal", new Item(new Item.Settings()));
    public static final Item FUEL = item("rocket_fuel", new Item(new Item.Settings()));
    public static final Item OXYGEN = item("oxygen_tank", new Item(new Item.Settings().maxDamage(600)));
    public static final RegistryEntry<ArmorMaterial> SPACE_ARMOR = registerSpaceArmor();
    public static final Item HELMET = item("space_helmet", new ArmorItem(SPACE_ARMOR, ArmorItem.Type.HELMET, new Item.Settings().maxDamage(165)));
    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ROCKET)).displayName(Text.translatable("itemGroup.lunar_horizons"))
            .entries((context, entries) -> {
                for (Block b : new Block[]{ROCKET, LAUNCH_PAD, REGOLITH, MOON_ROCK, CRYSTAL_ORE, MOON_BRICKS}) entries.add(b);
                for (Item i : new Item[]{FUEL, OXYGEN, HELMET, CRYSTAL}) entries.add(i);
            }).build();

    public static Identifier id(String path) { return Identifier.of(ID, path); }
    private static RegistryEntry<ArmorMaterial> registerSpaceArmor() {
        ArmorMaterial iron = ArmorMaterials.IRON.value();
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id("space_suit"),
                new ArmorMaterial(iron.defense(), iron.enchantability(), iron.equipSound(),
                        iron.repairIngredient(), List.of(new ArmorMaterial.Layer(id("space_suit"))),
                        iron.toughness(), iron.knockbackResistance()));
    }
    private static Item item(String path, Item item) { return Registry.register(Registries.ITEM, id(path), item); }
    private static Block block(String path, AbstractBlock.Settings settings) {
        Block block = Registry.register(Registries.BLOCK, id(path), new Block(settings));
        item(path, new BlockItem(block, new Item.Settings()));
        return block;
    }
    public static boolean hasHelmet(ServerPlayerEntity p) { return p.getEquippedStack(EquipmentSlot.HEAD).isOf(HELMET); }
    @Override public void onInitialize() {
        Registry.register(Registries.ITEM_GROUP, id("main"), GROUP);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity player && player.getWorld().getRegistryKey().equals(MOON) && source.isIn(DamageTypeTags.IS_FALL)));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player.isSpectator() || hand != Hand.MAIN_HAND || player.isSneaking() || !world.getBlockState(hit.getBlockPos()).isOf(ROCKET)) return ActionResult.PASS;
            if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
                RocketEntity.convertLegacy(serverPlayer, serverWorld, hit.getBlockPos());
            }
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MoonSurvival.tick(server);
        });
    }
}
