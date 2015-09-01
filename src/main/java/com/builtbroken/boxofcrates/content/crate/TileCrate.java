package com.builtbroken.boxofcrates.content.crate;

import com.builtbroken.mc.prefab.tile.Tile;
import com.builtbroken.mc.prefab.tile.TileInv;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;

/**
 * Basic inventory that stores one type of item but at a large volume
 * Created by Dark on 9/1/2015.
 */
public class TileCrate extends TileInv
{
    //TODO process inventory to get next empty slot so there is no need for creating a fake inventory
    protected int nextEmptySlot = 0;
    protected int currentStackSize = 0;
    protected ItemStack currentItem = null;

    public TileCrate()
    {
        super("crate", Material.iron);
    }

    @Override
    public Tile newTile()
    {
        return new TileCrate();
    }
}
