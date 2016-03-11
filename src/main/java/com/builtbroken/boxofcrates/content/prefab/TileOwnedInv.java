package com.builtbroken.boxofcrates.content.prefab;

import com.builtbroken.mc.api.items.tools.IItemLock;
import com.builtbroken.mc.api.tile.ITileLock;
import com.builtbroken.mc.prefab.tile.TileInv;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Prefab designed to make it easy to implemented inventory support with player ownership support
 * Created by Dark on 9/3/2015.
 */
public abstract class TileOwnedInv extends TileInv implements ITileLock
{
    protected ItemStack lockItemStack = null;
    protected ForgeDirection lockDisplaySide = ForgeDirection.NORTH;

    public TileOwnedInv(String name, Material material)
    {
        super(name, material);
    }

    @Override
    public boolean canLockWithItem(ItemStack stack)
    {
        return stack != null && stack.getItem() instanceof IItemLock;
    }

    @Override
    public ItemStack getLockItemStack()
    {
        return lockItemStack;
    }

    @Override
    public boolean isLocked()
    {
        return getLockItemStack() != null && getLockItemStack().getItem() instanceof IItemLock;
    }

    @Override
    public ItemStack lockTileWithStack(ItemStack stack)
    {
        if (lockItemStack == null && stack != null && stack.getItem() instanceof IItemLock)
        {
            this.lockItemStack = stack.copy();
            this.lockItemStack.stackSize = 1;
            stack.stackSize--;
            if (stack.stackSize <= 0)
            {
                return null;
            }
        }
        else if (stack == null)
        {
            this.lockItemStack = null;
        }
        return stack;
    }
}
