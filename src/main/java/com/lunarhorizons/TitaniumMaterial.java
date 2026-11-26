package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public enum TitaniumMaterial implements ToolMaterial {
    INSTANCE;
    public int getDurability(){return 1250;}
    public float getMiningSpeedMultiplier(){return 10.5F;}
    public float getAttackDamage(){return 2.5F;}
    public TagKey<Block> getInverseTag(){return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;}
    public int getEnchantability(){return 12;}
    public Ingredient getRepairIngredient(){return Ingredient.ofItems(LunarHorizons.TITANIUM_INGOT);}
}
