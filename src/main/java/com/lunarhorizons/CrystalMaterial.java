package com.lunarhorizons;

import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public enum CrystalMaterial implements ToolMaterial {
    INSTANCE;
    public int getDurability(){return 920;}
    public float getMiningSpeedMultiplier(){return 7.5F;}
    public float getAttackDamage(){return 2.5F;}
    public TagKey<Block> getInverseTag(){return BlockTags.INCORRECT_FOR_IRON_TOOL;}
    public int getEnchantability(){return 16;}
    public Ingredient getRepairIngredient(){return Ingredient.ofItems(LunarHorizons.CRYSTAL);}
}
