package com.builtbroken.boxofcrates.content.crate;

import com.builtbroken.boxofcrates.BoxOfCrates;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.imp.transform.vector.Location;
import com.builtbroken.mc.imp.transform.vector.Pos;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
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
    public HashMap<Integer, ItemStack> inventory = new HashMap();
    //TODO replace with arrayList, or single stack
    /** Set of slots that have space to add items */
    public SortedSet<Integer> slotsWithRoomStack = new TreeSet();

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
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        ItemStack heldItem = player.getHeldItem() != null ? player.getHeldItem().copy() : null;
        boolean contentsChanged = false;

        if (dir != ForgeDirection.DOWN)
        {
            //Add Items
            if (dir == ForgeDirection.UP || hit.y() >= 5)
            {
                if (isValid(heldItem))
                {
                    if (currentItem == null)
                    {
                        currentItem = heldItem.copy();
                        currentItem.stackSize = 1;
                    }
                    int roomLeft = (getSizeInventory() * getInventoryStackLimit()) - currentStackSize;
                    if (roomLeft > 0)
                    {
                        if (isServer())
                        {
                            //Creative mode mass fill option
                            if (dir == ForgeDirection.UP && player.capabilities.isCreativeMode)
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
                if (isValid(heldItem) && heldItem.stackSize < heldItem.getMaxStackSize())
                {
                    int itemsToRemove = player.isSneaking() ? 1 : heldItem.getMaxStackSize() - heldItem.stackSize;
                    if (itemsToRemove <= currentStackSize)
                    {
                        heldItem.stackSize += itemsToRemove;
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, heldItem);
                        player.inventoryContainer.detectAndSendChanges();
                        decreaseCount(itemsToRemove);
                    }
                    else
                    {
                        heldItem.stackSize += currentStackSize;
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, heldItem);
                        player.inventoryContainer.detectAndSendChanges();
                        clearInventory();
                    }
                }
                //Add to player's inventory
                else
                {
                    int itemsToRemove = player.isSneaking() ? 1 : 64;
                    if (itemsToRemove <= currentStackSize)
                    {
                        heldItem = currentItem.copy();
                        heldItem.stackSize = itemsToRemove;
                        if (!player.inventory.addItemStackToInventory(heldItem))
                        {
                            InventoryUtility.dropItemStack(new Location(player), heldItem);
                        }
                        player.inventoryContainer.detectAndSendChanges();
                        decreaseCount(itemsToRemove);
                    }
                    else
                    {
                        heldItem = currentItem.copy();
                        heldItem.stackSize = currentStackSize;
                        if (!player.inventory.addItemStackToInventory(heldItem))
                        {
                            InventoryUtility.dropItemStack(new Location(player), heldItem);
                        }
                        player.inventoryContainer.detectAndSendChanges();
                        clearInventory();
                    }
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
        if (currentItem != null && count > 0)
        {
            while (count > 0)
            {
                int slot = getNextEmptySlot();
                if (slot == -2)
                    break;
                ItemStack slotContent = getStackInSlot(slot);
                int roomLeft = slotContent != null ? getInventoryStackLimit() - slotContent.stackSize : getInventoryStackLimit();
                int setSize = Math.min(roomLeft, count);
                count -= setSize;
                if (slotContent == null)
                {
                    slotContent = currentItem.copy();
                    slotContent.stackSize = setSize;
                    setInventorySlotContents(slot, slotContent);
                }
                else
                {
                    slotContent.stackSize += setSize;
                    currentStackSize += setSize;
                }
            }
        }
    }

    public void decreaseCount(int count)
    {
        if (currentItem != null && count > 0)
        {
            currentStackSize -= count;
            if (currentStackSize > 0)
            {
                int itemsToRemove = count;

                //http://stackoverflow.com/questions/922528/how-to-sort-map-values-by-key-in-java
                List sortedKeys = new ArrayList(inventory.keySet());
                Collections.sort(sortedKeys);

                for (int i = sortedKeys.size() - 1; i >= 0; i--)
                {
                    if (itemsToRemove == 0)
                        break; //Done all items removed
                    if (getStackInSlot(i) == null)
                    {
                        inventory.remove(i);
                        slotsWithRoomStack.add(i);
                        if (nextEmptySlot > i)
                            nextEmptySlot = i;
                    }
                    else if (getStackInSlot(i).stackSize <= itemsToRemove)
                    {
                        itemsToRemove -= getStackInSlot(i).stackSize;
                        inventory.remove(i);
                        slotsWithRoomStack.add(i);
                        if (nextEmptySlot > i)
                            nextEmptySlot = i;
                    }
                    else
                    {
                        getStackInSlot(i).stackSize -= itemsToRemove;
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

    /**
     * Wipes out all stored data about the inventory
     */
    public void clearInventory()
    {
        currentItem = null;
        currentStackSize = 0;
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

                    //If we filled only part of the slot then we need to add it to the slot stack
                    if (itemsLeft == 0 && newStack.stackSize < getInventoryStackLimit())
                    {
                        if (nextEmptySlot == -1)
                            nextEmptySlot = i;
                        slotsWithRoomStack.add(i);
                    }

                    //Handle last slot
                    if (i == getSizeInventory() - 1)
                    {
                        //If last slot is filled then there are not slots left, note with -2
                        if (newStack.stackSize == getInventoryStackLimit())
                            nextEmptySlot = -2;
                            //If there is room this is our next free slot
                        else
                            nextEmptySlot = i;
                    }
                }
                //We have no items left but our slot stack is not fully initialized, lets add remaining slots to add
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
                if (stack == null || stack.stackSize < getInventoryStackLimit())
                {
                    if (nextEmptySlot < 0)
                        nextEmptySlot = i;
                    if (!slotsWithRoomStack.contains(i))
                        slotsWithRoomStack.add(i);
                }
            }
        }
        ItemStack stack = getStackInSlot(nextEmptySlot);
        if (stack != null && stack.stackSize >= getInventoryStackLimit())
        {
            slotsWithRoomStack.remove(nextEmptySlot);
            nextEmptySlot = -2;
            Iterator<Integer> it = slotsWithRoomStack.iterator();
            while (it.hasNext())
            {
                int slot = it.next();
                stack = getStackInSlot(slot);
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
        if (stack != null && amount > 0)
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
        if (slot >= 0 && slot < getSizeInventory())
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
        else if (slot < 0)
        {
            Engine.error(this + " Error: slot passed in should not be negative. Report this to a mod author as something is miss using IInventory API to force slot updates.");
        }
        else if (slot >= getSizeInventory())
        {
            Engine.error(this + " Error: slot passed in should not be larger than the inventory size. Report this to a mod author as something is miss using IInventory API to force slot updates.");
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
        return slot >= 0 && slot < getSizeInventory() && isValid(stack);
    }

    public boolean isValid(ItemStack stack)
    {
        return stack != null && stack.stackSize > 0 && (doesItemStackMatch(stack) || currentItem == null);
    }

    /**
     * Checks to see if the stack matches {@link TileCrate#currentItem}
     *
     * @param stack - ItemStack, can handle null values
     * @return true if the stack matches
     */
    public boolean doesItemStackMatch(ItemStack stack)
    {
        return currentItem == null && stack == null || stack != null && currentItem != null && currentItem.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(currentItem, stack);
    }

    @Override
    public Tile newTile()
    {
        return new TileCrate();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side)
    {
        return side >= 0 && side < 6 ? crateType.slots : EMPTY_INT_ARRAY;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side)
    {
        return slot >= 0 && slot < getSizeInventory() && isValid(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side)
    {
        return slot >= 0 && slot < getSizeInventory();
    }

    @Override
    public String toString()
    {
        return String.format("Crate[%dx %dy %dz]%d", xi(), yi(), zi(), hashCode());
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
