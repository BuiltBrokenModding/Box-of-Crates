package com.builtbroken.boxofcrates.content.crate;

import com.builtbroken.boxofcrates.BoxOfCrates;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.lib.transform.vector.Pos;
import com.builtbroken.mc.prefab.tile.Tile;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

/**
 * Basic inventory that stores one type of item but at a large volume
 * Created by Dark on 9/1/2015.
 */
public class TileCrate extends Tile implements ISidedInventory
{
    /** Slot ID for the next slot with room to store items. -1 means that the inventory has not been initialized for data. -2 means the inventory has no free slots. */
    protected int nextEmptySlot = -1;
    /** Current number of items stored in the inventory. Its mainly used for packet updates and display data */
    public int currentStackSize = 0;
    /** Current ItemStack in the crate. Stack size is always 1 and its only used for logic checks. */
    public ItemStack currentItem = null;

    /** Tier/Type of crate */
    public CrateType crateType = CrateType.BASE;

    /** Main inventory map */
    private HashMap<Integer, ItemStack> inventory = new HashMap();
    /** Set of slots that have space to add items */
    private SortedSet<Integer> slotsWithRoomStack = new TreeSet();

    public TileCrate()
    {
        super("crate", Material.iron);
    }

    @Override
    public void firstTick()
    {
        super.firstTick();
        this.crateType = CrateType.values()[world().getBlockMetadata(xi(), yi(), zi())];
    }

    @Override
    protected boolean onPlayerRightClick(EntityPlayer player, int side, Pos hit)
    {
        if (!super.onPlayerRightClick(player, side, hit))
        {
            ForgeDirection dir = ForgeDirection.getOrientation(side);
            ItemStack heldItem = player.getHeldItem() != null ? player.getHeldItem().copy() : null;
            boolean contentsChanged = false;

            if (dir != ForgeDirection.SOUTH)
            {
                //Add Items
                if (dir == ForgeDirection.NORTH || hit.y() >= 4.9)
                {
                    if (isValid(heldItem))
                    {
                        int roomLeft = (getSizeInventory() * getInventoryStackLimit()) - currentStackSize;
                        if (roomLeft != 0)
                        {
                            if (isServer())
                            {
                                //Creative mode mass fill option
                                if (dir == ForgeDirection.NORTH && player.capabilities.isCreativeMode)
                                {
                                    currentStackSize = getSizeInventory() * getInventoryStackLimit();
                                    contentsChanged = true;
                                    player.addChatComponentMessage(new ChatComponentText(LanguageUtility.getLocalName("crate.onRightClick.creativeFilled")));
                                }
                                else
                                {
                                    int heldStackSize = player.isSneaking() ? 1 : heldItem.stackSize;
                                    if (heldStackSize > roomLeft)
                                    {
                                        heldItem.stackSize -= roomLeft;
                                        increaseCount(roomLeft);
                                    }
                                    else
                                    {
                                        heldItem.stackSize -= heldStackSize;
                                        increaseCount(heldStackSize);
                                    }
                                    if (heldItem.stackSize > 0)
                                        player.inventory.setInventorySlotContents(player.inventory.currentItem, heldItem);
                                    else
                                        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                                    player.inventoryContainer.detectAndSendChanges();
                                    contentsChanged = true;
                                }
                            }
                        }
                        else if (isServer())
                        {
                            player.addChatComponentMessage(new ChatComponentText(LanguageUtility.getLocalName("crate.onRightClick.error.full")));
                        }
                    }
                    else if (isServer())
                    {
                        player.addChatComponentMessage(new ChatComponentText(LanguageUtility.getLocalName("crate.onRightClick.error.invalidStack")));
                    }
                }
                //Remove Items
                else if (currentItem != null && currentStackSize > 0)
                {
                    //Fill held item
                    if (isValid(heldItem) && heldItem.stackSize < heldItem.getMaxStackSize() || heldItem == null)
                    {
                        int itemsToRemove = player.isSneaking() ? 1 : currentItem.getMaxStackSize() - heldItem.getMaxStackSize();
                        if (itemsToRemove <= currentStackSize)
                        {
                            heldItem.stackSize += itemsToRemove;
                            player.inventoryContainer.detectAndSendChanges();
                            decreaseCount(itemsToRemove);
                        }
                        else
                        {
                            heldItem.stackSize += currentStackSize;
                            player.inventoryContainer.detectAndSendChanges();
                            clearInventory();
                        }
                    }
                    //Add to inventory
                    else
                    {

                    }
                }

                if (contentsChanged)
                {
                    if (isServer())
                    {
                        //TODO send packet
                    }
                    else
                    {
                        //TODO maybe call a re-render of the tile?
                    }
                }
                return true;
            }
            else
            {
                if (isServer())
                    player.addChatComponentMessage(new ChatComponentText(LanguageUtility.getLocalName("crate.onRightClick.bottom.error")));
                return true;
            }
        }
        return true;
    }

    /**
     * Adds items to inventory in the first slots it can find.
     * Make sure that the currentItem matches the input item as this
     * doesn't check anything. It only iterates threw free slots filling
     * the slots to cap.
     *
     * @param count - number of items to add
     */
    public void increaseCount(int count)
    {
        if (currentItem != null)
        {
            this.currentStackSize += count;
            while (count > 0)
            {
                int slot = getNextEmptySlot();
                if (slot == -2)
                    break;
                ItemStack slotContent = getStackInSlot(slot);
                int roomLeft = slotContent != null ? getInventoryStackLimit() - slotContent.stackSize : getInventoryStackLimit();
            }
        }
    }

    public void decreaseCount(int count)
    {
        if (count > 0)
        {
            if (currentItem != null)
            {
                currentStackSize -= count;
                if (currentStackSize > 0)
                {
                    int itemsToRemove = count;
                    Iterator<Map.Entry<Integer, ItemStack>> it = inventory.entrySet().iterator();
                    while (it.hasNext())
                    {
                        Map.Entry<Integer, ItemStack> entry = it.next();
                        if (itemsToRemove == 0)
                            break; //Done all items removed
                        if (entry.getValue() == null)
                        {
                            it.remove();
                            slotsWithRoomStack.add(entry.getKey());
                            if (nextEmptySlot < 0)
                                nextEmptySlot = entry.getKey();
                        }
                        else if (entry.getValue().stackSize <= itemsToRemove)
                        {
                            itemsToRemove -= entry.getValue().stackSize;
                            it.remove();
                            slotsWithRoomStack.add(entry.getKey());
                            if (nextEmptySlot < 0)
                                nextEmptySlot = entry.getKey();
                        }
                        else
                        {
                            entry.getValue().stackSize -= itemsToRemove;
                            break; //Done all items removed
                        }
                    }
                }
                else
                {
                    clearInventory();
                }
            }
        }
    }

    /**
     * Wipes out all stored data about the inventory
     */
    public void clearInventory()
    {
        currentItem = null;
        inventory.clear();
        slotsWithRoomStack.clear();
        nextEmptySlot = -1;
    }


    /**
     * Cleans up the inventory rebuilding it from
     * the CurrentItem and CurrentStackSize values
     */
    public void rebuildEntireInventory()
    {
        //Clear inventory for rebuild
        inventory.clear();
        slotsWithRoomStack.clear();
        nextEmptySlot = -1;

        if (currentItem != null && currentStackSize > 0)
        {
            //Ensure max inventory limit
            currentStackSize = Math.min(currentStackSize, getInventoryStackLimit() * getSizeInventory());

            //cache items to go
            int itemsLeft = currentStackSize;
            for (int i = 0; i < getSizeInventory(); i++)
            {
                if (itemsLeft > 0)
                {
                    ItemStack newStack = currentItem.copy();
                    newStack.stackSize = Math.min(getInventoryStackLimit(), itemsLeft);
                    inventory.put(i, newStack);
                    itemsLeft -= newStack.stackSize;
                    if (i == getSizeInventory() - 1)
                    {
                        if (newStack.stackSize == getInventoryStackLimit())
                            nextEmptySlot = -2;
                        else
                            nextEmptySlot = i;
                    }
                }
                else
                {
                    if (nextEmptySlot == -1)
                        nextEmptySlot = i;
                    slotsWithRoomStack.add(i);
                }
            }
        }
        else
        {
            currentItem = null;
        }
    }

    public int getNextEmptySlot()
    {
        //Find all empty slots
        if (nextEmptySlot == -1)
        {
            nextEmptySlot = -2;
            slotsWithRoomStack.clear();
            for (int i = 0; i < getSizeInventory(); i++)
            {
                ItemStack stack = getStackInSlot(i);
                if (stack == null || stack.stackSize < getSizeInventory())
                {
                    if (nextEmptySlot == -1)
                        nextEmptySlot = i;
                    if (!slotsWithRoomStack.contains(i))
                        slotsWithRoomStack.add(i);
                }
            }
        }
        ItemStack stack = getStackInSlot(nextEmptySlot);
        if (stack != null && stack.stackSize >= getSizeInventory())
        {
            slotsWithRoomStack.remove(nextEmptySlot);
            nextEmptySlot = -2;
            Iterator<Integer> it = slotsWithRoomStack.iterator();
            while (it.hasNext())
            {
                int slot = it.next();
                if (stack == null || stack.stackSize < getSizeInventory())
                {
                    nextEmptySlot = slot;
                    break;
                }
                else
                {
                    it.remove();
                }
            }
        }
        return nextEmptySlot;
    }

    @Override
    public int getSizeInventory()
    {
        return crateType.size;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        if (inventory.containsKey(slot))
        {
            return inventory.get(slot);
        }
        return null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount)
    {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null && amount != 0)
        {
            if (stack.stackSize <= amount)
            {
                setInventorySlotContents(slot, null);
            }
            else
            {
                stack = stack.splitStack(amount);
                setInventorySlotContents(slot, getStackInSlot(slot));
            }
            markDirty();
            return stack;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot)
    {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null)
        {
            inventory.remove(slot);
            return stack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack)
    {
        boolean change = false;
        ItemStack prev_stack = getStackInSlot(slot);
        if (stack != null && stack.stackSize > 0)
        {
            inventory.put(slot, stack.copy());
        }
        else
        {
            inventory.remove(slot);
        }

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
            currentStackSize -= prev_stack.stackSize;
            if (currentStackSize <= 0)
            {
                clearInventory();
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
            markDirty();
        }
    }

    @Override
    public String getInventoryName()
    {
        return "crate.container";
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return true;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return currentItem != null ? currentItem.getMaxStackSize() : 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player)
    {
        return toPos().add(0.5).distance(player) < 5;
    }

    @Override
    public void openInventory()
    {

    }

    @Override
    public void closeInventory()
    {

    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return slot < getSizeInventory() && isValid(stack);
    }

    protected boolean isValid(ItemStack stack)
    {
        return doesItemStackMatch(stack) || currentItem == null && stack != null;
    }

    protected boolean doesItemStackMatch(ItemStack stack)
    {
        return stack != null && currentItem.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(currentItem, stack);
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
        return isValid(stack);
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
