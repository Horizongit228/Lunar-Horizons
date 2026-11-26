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
import net.minecraft.item.SwordItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
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
    public static final Block DARK_CRYSTAL_ORE = block("dark_crystal_ore", AbstractBlock.Settings.copy(Blocks.DEEPSLATE_DIAMOND_ORE).requiresTool().luminance(s -> 3));
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
    public static final Item DARK_CRYSTAL = item("dark_crystal", new Item(new Item.Settings()));
    public static final Item FUEL = item("rocket_fuel", new Item(new Item.Settings()));
    public static final Item OXYGEN = item("oxygen_tank", new Item(new Item.Settings().maxDamage(600)));
    public static final Item LARGE_OXYGEN = item("large_oxygen_tank",new Item(new Item.Settings().maxDamage(1800)));
    public static final Item BACKPACK = item("oxygen_backpack",new Item(new Item.Settings().maxCount(1)));
    public static final Block REFILL_STATION = block("oxygen_station",AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).luminance(s -> 4));
    public static final Item LUNAR_STICK = item("lunar_stick",new Item(new Item.Settings()));
    public static final Block LUNAR_STEMS = block("lunar_stems",AbstractBlock.Settings.copy(Blocks.AMETHYST_CLUSTER).strength(.5F).noCollision().nonOpaque().luminance(s -> 2));
    public static final Feature<DefaultFeatureConfig> LUNAR_STEM_PATCH = Registry.register(Registries.FEATURE,id("lunar_stem_patch"),new LunarStemFeature(DefaultFeatureConfig.CODEC));
    public static final Block CRYSTAL_CLUSTER = block("crystal_cluster",AbstractBlock.Settings.copy(Blocks.AMETHYST_CLUSTER).noCollision().nonOpaque().luminance(s -> 12));
    public static final Block CRYSTAL_STONE = block("crystal_stone",AbstractBlock.Settings.copy(Blocks.AMETHYST_BLOCK).luminance(s -> 8));
    public static final Feature<DefaultFeatureConfig> CAVE_ENTRANCE = Registry.register(Registries.FEATURE,id("cave_entrance"),new CaveEntranceFeature(DefaultFeatureConfig.CODEC));
    public static final Feature<DefaultFeatureConfig> CRYSTAL_GROTTO = Registry.register(Registries.FEATURE,id("crystal_grotto"),new CrystalGrottoFeature(DefaultFeatureConfig.CODEC));
    public static final RegistryEntry<ArmorMaterial> SPACE_ARMOR = registerSpaceArmor();
    public static final Item HELMET = item("space_helmet", new ArmorItem(SPACE_ARMOR, ArmorItem.Type.HELMET, new Item.Settings().maxDamage(165)));
    public static final Item CHEST = item("space_chestplate",new ArmorItem(SPACE_ARMOR,ArmorItem.Type.CHESTPLATE,new Item.Settings().maxDamage(240)));
    public static final Item LEGS = item("space_leggings",new ArmorItem(SPACE_ARMOR,ArmorItem.Type.LEGGINGS,new Item.Settings().maxDamage(225)));
    public static final Item BOOTS = item("space_boots",new ArmorItem(SPACE_ARMOR,ArmorItem.Type.BOOTS,new Item.Settings().maxDamage(195)));
    public static final Item CRYSTAL_SWORD = item("crystal_sword",new SwordItem(CrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(SwordItem.createAttributeModifiers(CrystalMaterial.INSTANCE,3,-2.4F))));
    public static final Item CRYSTAL_PICKAXE = item("crystal_pickaxe",new PickaxeItem(CrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(PickaxeItem.createAttributeModifiers(CrystalMaterial.INSTANCE,1,-2.8F))));
    public static final Item CRYSTAL_AXE = item("crystal_axe",new AxeItem(CrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(AxeItem.createAttributeModifiers(CrystalMaterial.INSTANCE,6,-3.1F))));
    public static final Item CRYSTAL_SHOVEL = item("crystal_shovel",new ShovelItem(CrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(ShovelItem.createAttributeModifiers(CrystalMaterial.INSTANCE,1.5F,-3.0F))));
    public static final Item CRYSTAL_HOE = item("crystal_hoe",new HoeItem(CrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(CrystalMaterial.INSTANCE,-2,-1.0F))));
    public static final Item DARK_CRYSTAL_SWORD = item("dark_crystal_sword",new SwordItem(DarkCrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(SwordItem.createAttributeModifiers(DarkCrystalMaterial.INSTANCE,3,-2.4F))));
    public static final Item DARK_CRYSTAL_PICKAXE = item("dark_crystal_pickaxe",new PickaxeItem(DarkCrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(PickaxeItem.createAttributeModifiers(DarkCrystalMaterial.INSTANCE,1,-2.8F))));
    public static final Item DARK_CRYSTAL_AXE = item("dark_crystal_axe",new AxeItem(DarkCrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(AxeItem.createAttributeModifiers(DarkCrystalMaterial.INSTANCE,6,-3.1F))));
    public static final Item DARK_CRYSTAL_SHOVEL = item("dark_crystal_shovel",new ShovelItem(DarkCrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(ShovelItem.createAttributeModifiers(DarkCrystalMaterial.INSTANCE,1.5F,-3.0F))));
    public static final Item DARK_CRYSTAL_HOE = item("dark_crystal_hoe",new HoeItem(DarkCrystalMaterial.INSTANCE,new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(DarkCrystalMaterial.INSTANCE,-4,0.0F))));
    public static final Block HIGHLAND_ROCK = block("highland_rock",AbstractBlock.Settings.copy(Blocks.STONE));
    public static final Block DARK_REGOLITH = block("dark_regolith",AbstractBlock.Settings.copy(Blocks.DEEPSLATE));
    public static final Block FROSTED_REGOLITH = block("frosted_regolith",AbstractBlock.Settings.copy(Blocks.DIRT));
    public static final Block LUNAR_ICE = block("lunar_ice",AbstractBlock.Settings.copy(Blocks.PACKED_ICE));
    public static final Block TITANIUM_ORE = block("titanium_ore",AbstractBlock.Settings.copy(Blocks.IRON_ORE));
    public static final Block HELIUM_ORE = block("helium_ore",AbstractBlock.Settings.copy(Blocks.COAL_ORE).luminance(s -> 2));
    public static final Block METEORITE_ROCK = block("meteorite_rock",AbstractBlock.Settings.copy(Blocks.DEEPSLATE));
    public static final Block LUNAR_LANTERN = block("lunar_lantern",AbstractBlock.Settings.copy(Blocks.SEA_LANTERN).luminance(s -> 15));
    public static final Block SURVEY_BEACON = block("survey_beacon",AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).luminance(s -> 10));
    public static final Block TITANIUM_BLOCK = block("titanium_block",AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
    public static final Item RAW_TITANIUM = item("raw_titanium",new Item(new Item.Settings()));
    public static final Item TITANIUM_INGOT = item("titanium_ingot",new Item(new Item.Settings()));
    public static final Item HELIUM = item("helium_3",new Item(new Item.Settings()));
    public static final Item METEORITE_FRAGMENT = item("meteorite_fragment",new Item(new Item.Settings()));
    public static final Item EXTREME_OXYGEN = item("extreme_oxygen_tank",new Item(new Item.Settings().maxDamage(3600)));
    public static final Item MINERAL_DETECTOR = item("mineral_detector",new SurveyItem(new Item.Settings().maxCount(1),false));
    public static final Item LUNAR_RADAR = item("lunar_radar",new SurveyItem(new Item.Settings().maxCount(1),true));
    public static final Item TITANIUM_DRILL = item("titanium_drill",new PickaxeItem(TitaniumMaterial.INSTANCE,new Item.Settings().attributeModifiers(PickaxeItem.createAttributeModifiers(TitaniumMaterial.INSTANCE,1,-2.8F))));
    public static final Feature<DefaultFeatureConfig> CRATER = Registry.register(Registries.FEATURE,id("impact_crater"),new ImpactCraterFeature(DefaultFeatureConfig.CODEC));
    public static final Feature<DefaultFeatureConfig> OUTPOST = Registry.register(Registries.FEATURE,id("abandoned_outpost"),new ExplorationSiteFeature(DefaultFeatureConfig.CODEC,0));
    public static final Feature<DefaultFeatureConfig> SATELLITE = Registry.register(Registries.FEATURE,id("crashed_satellite"),new ExplorationSiteFeature(DefaultFeatureConfig.CODEC,1));
    public static final Feature<DefaultFeatureConfig> LAB = Registry.register(Registries.FEATURE,id("buried_lab"),new ExplorationSiteFeature(DefaultFeatureConfig.CODEC,2));
    public static final Feature<DefaultFeatureConfig> GEODE = Registry.register(Registries.FEATURE,id("lunar_geode"),new ExplorationSiteFeature(DefaultFeatureConfig.CODEC,3));
    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ROCKET)).displayName(Text.translatable("itemGroup.lunar_horizons"))
            .entries((context, entries) -> {
                for (Block b : new Block[]{ROCKET, LAUNCH_PAD, REGOLITH, MOON_ROCK, CRYSTAL_ORE, MOON_BRICKS, REFILL_STATION, CRYSTAL_CLUSTER, CRYSTAL_STONE, DARK_CRYSTAL_ORE, LUNAR_STEMS, HIGHLAND_ROCK, DARK_REGOLITH, FROSTED_REGOLITH, LUNAR_ICE, TITANIUM_ORE, HELIUM_ORE, METEORITE_ROCK, LUNAR_LANTERN, SURVEY_BEACON, TITANIUM_BLOCK}) entries.add(b);
                for (Item i : new Item[]{FUEL, OXYGEN, LARGE_OXYGEN, BACKPACK, HELMET, CHEST, LEGS, BOOTS, LUNAR_STICK, CRYSTAL, CRYSTAL_SWORD, CRYSTAL_PICKAXE, CRYSTAL_AXE, CRYSTAL_SHOVEL, CRYSTAL_HOE, DARK_CRYSTAL, DARK_CRYSTAL_SWORD, DARK_CRYSTAL_PICKAXE, DARK_CRYSTAL_AXE, DARK_CRYSTAL_SHOVEL, DARK_CRYSTAL_HOE, RAW_TITANIUM, TITANIUM_INGOT, HELIUM, METEORITE_FRAGMENT, EXTREME_OXYGEN, MINERAL_DETECTOR, LUNAR_RADAR, TITANIUM_DRILL}) entries.add(i);
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
        RocketControl.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> MeteorEvents.clear());
        Registry.register(Registries.ITEM_GROUP, id("main"), GROUP);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity player && player.getWorld().getRegistryKey().equals(MOON) && source.isIn(DamageTypeTags.IS_FALL)));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if(!player.isSpectator() && hand==Hand.MAIN_HAND && world.getBlockState(hit.getBlockPos()).isOf(REFILL_STATION) && OxygenSystem.isTank(player.getStackInHand(hand))) {
                if(player instanceof ServerPlayerEntity p) OxygenSystem.refill(p, p.getStackInHand(hand));
                return ActionResult.SUCCESS;
            }
            if (player.isSpectator() || hand != Hand.MAIN_HAND || player.isSneaking() || !world.getBlockState(hit.getBlockPos()).isOf(ROCKET)) return ActionResult.PASS;
            if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
                RocketEntity.convertLegacy(serverPlayer, serverWorld, hit.getBlockPos());
            }
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MoonSurvival.tick(server);
            MeteorEvents.tick(server);
        });
    }
}
