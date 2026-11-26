package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public enum DarkCrystalMaterial implements ToolMaterial {
    INSTANCE;
    public int getDurability(){return 1900;}
    public float getMiningSpeedMultiplier(){return 9.5F;}
    public float getAttackDamage(){return 4.0F;}
    public TagKey<Block> getInverseTag(){return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;}
    public int getEnchantability(){return 18;}
    public Ingredient getRepairIngredient(){return Ingredient.ofItems(LunarHorizons.DARK_CRYSTAL);}
}
