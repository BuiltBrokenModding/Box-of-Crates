package com.builtbroken.boxofcrates.content.crate;

import com.builtbroken.boxofcrates.BoxOfCrates;
import com.builtbroken.mc.prefab.tile.Tile;
import com.builtbroken.mc.prefab.tile.TileInv;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/**
 * Basic inventory that stores one type of item but at a large volume
 * Created by Dark on 9/1/2015.
 */
public class TileCrate extends TileInv implements ISidedInventory
{
    //TODO process inventory to get next empty slot so there is no need for creating a fake inventory
    protected int nextEmptySlot = 0;
    protected int currentStackSize = 0;
    protected ItemStack currentItem = null;

    private CrateType crateType = CrateType.BASE;

    public TileCrate()
    {
        super("crate", Material.iron);
    }

    @Override
    public void firstTick()
    {
        super.firstTick();
        this.crateType = CrateType.values()[world().getBlockMetadata(xi(), yi(), zi())];
        this.slots_$eq(crateType.size);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack)
    {
        boolean change = false;
        ItemStack prev_stack = getStackInSlot(slot);
        super.setInventorySlotContents(slot, stack);

        //Set item stack cache
        if (currentItem == null && stack != null && stack.stackSize > 0)
        {
            currentItem = stack.copy();
            currentItem.stackSize = 1;
        }

        //Slot was empty
        if (prev_stack == null && stack != null && stack.stackSize > 0)
        {
            currentStackSize += stack.stackSize;
            change = true;
        }
        //Slot contained content but is being set to null
        else if (prev_stack != null && stack == null)
        {
            currentStackSize -= stack.stackSize;
            if (currentStackSize <= 0)
            {
                currentItem = null;
            }
            change = true;
        }
        //Slot was not empty but is being updated
        else if (prev_stack != null && stack != null && stack.stackSize > 0)
        {
            if (!currentItem.isItemEqual(stack) || !ItemStack.areItemStackTagsEqual(currentItem, stack))
            {
                BoxOfCrates.INSTANCE.logger().error(this + " Something attempted to set slot " + slot + " to stack " + stack + " which doesn't match " + currentItem + ". Ignoring set to prevent duplication. Items were most likely lost in this interaction and need to be returned by an admin. Report this error and following exception to the mod developer.", new RuntimeException());
                return;
            }
            if (prev_stack.stackSize < stack.stackSize)
            {
                currentStackSize += prev_stack.stackSize - stack.stackSize;
                change = true;
            }
            else if (prev_stack.stackSize > stack.stackSize)
            {
                currentStackSize -= stack.stackSize - prev_stack.stackSize;
                change = true;
            }
        }

        if (change && isServer())
        {
            //TODO send packet
        }
    }

    @Override
    public Tile newTile()
    {
        return new TileCrate();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side)
    {
        return crateType.slots;
    }

    @Override
    public boolean canInsertItem(int p_102007_1_, ItemStack stack, int p_102007_3_)
    {
        return stack != null && (currentItem == null || currentItem.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(currentItem, stack));
    }

    @Override
    public boolean canExtractItem(int p_102008_1_, ItemStack stack, int p_102008_3_)
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "Crate[" + xi() + "x " + yi() + "y " + zi() + "z ]" + hashCode();
    }

    public enum CrateType
    {
        BASE(32),
        ADVANCED(64),
        ELITE(256);

        public final int size;
        public final int[] slots;

        CrateType(int size)
        {
            this.size = size;
            slots = new int[size];
            for (int i = 0; i < size; i++)
            {
                slots[i] = i;
            }
        }
    }

}
