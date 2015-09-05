package com.builtbroken.test.boxofcrates;

import com.builtbroken.boxofcrates.content.crate.TileCrate;
import com.builtbroken.mc.lib.transform.vector.Pos;
import com.builtbroken.mc.prefab.items.ItemStackWrapper;
import com.builtbroken.mc.prefab.tile.entity.TileEntityBase;
import com.builtbroken.mc.testing.junit.AbstractTest;
import com.builtbroken.mc.testing.junit.VoltzTestRunner;
import com.builtbroken.mc.testing.junit.server.FakeDedicatedServer;
import com.builtbroken.mc.testing.junit.testers.TestPlayer;
import com.builtbroken.mc.testing.junit.world.FakeWorldServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * JUnit test for {@link com.builtbroken.boxofcrates.content.crate.TileCrate}
 * Created by Dark on 9/3/2015.
 */
@RunWith(VoltzTestRunner.class)
public class TestCrate extends AbstractTest
{
    //TODO create a test group to run base level code that all tiles contain
    /** World to do testing in */
    private static FakeWorldServer world;
    /** Player to use for testing interaction methods */
    private static TestPlayer player;

    @Override
    public void setUpForEntireClass()
    {
        //There is a possibility this has some lingering data attached to it due to MC's data flow
        MinecraftServer server = new FakeDedicatedServer(new File(FakeWorldServer.baseFolder, "CrateTester"));
        world = FakeWorldServer.newWorld(server, "TestCrate");
        player = new TestPlayer(server, world, new GameProfile(null, "CrateTester"));
    }

    @Override
    public void setUpForTest(String name)
    {
        player.reset();
    }

    /** Tests {@link TileCrate#TileCrate()}, {@link TileCrate#newTile()} ()}, {@link TileCrate#toString()} ()} */
    public void testInit()
    {
        //Mainly testing stuff that should work but needs tested for code coverage percentage
        TileCrate crate = new TileCrate();
        assertTrue("Should have received a new instance of a crate", crate.newTile() instanceof TileCrate);
        String excpected = "Crate[0x 0y 0z]" + crate.hashCode();
        String re = crate.toString();
        assertTrue("toString() should have returned " + excpected + " but got " + re, re.equals(excpected));
    }

    /** Tests an internal method {@link TestCrate#areItemStacksEqual(ItemStack, ItemStack)} */
    public void testItemStackEqual()
    {
        assertTrue(areItemStacksEqual(new ItemStack(Items.apple), new ItemStack(Items.apple)));
        assertFalse(areItemStacksEqual(new ItemStack(Items.apple), new ItemStack(Items.stick)));
    }

    /** Tests {@link TileCrate#getStackInSlot(int)} and {@link TileCrate#setInventorySlotContents(int, ItemStack)} */
    public void testInventorySlots()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        for (TileCrate.CrateType type : TileCrate.CrateType.values())
        {
            crate.crateType = type;
            for (ItemStack stack : new ItemStack[]{new ItemStack(Items.apple), new ItemStack(Items.stone_axe)})
            {
                for (int i = 0; i < crate.getSizeInventory(); i++)
                {
                    //Check to ensure slot is empty
                    assertTrue("Slot " + i + " should be empty", crate.getStackInSlot(i) == null);

                    //Set slot to stack
                    crate.setInventorySlotContents(i, stack);
                    assertTrue("Slot " + i + " should contain " + stack, areItemStacksEqual(crate.getStackInSlot(i), stack));
                    assertTrue("Current stack should be " + stack, areItemStacksEqual(crate.getStackInSlot(i), stack));
                    assertTrue("Slot limit should equal " + stack.getMaxStackSize(), crate.getInventoryStackLimit() == stack.getMaxStackSize());

                    //Cleanup
                    crate.setInventorySlotContents(i, null);
                    assertTrue("Slot " + i + " should be empty", crate.getStackInSlot(i) == null);
                    assertTrue("Current stack should be null", crate.currentItem == null);
                }

                assertTrue("Slot -1 should always return null", crate.getStackInSlot(-1) == null);
                assertTrue("Slot " + crate.getSizeInventory() + " should always return null", crate.getStackInSlot(crate.getSizeInventory()) == null);

                try
                {
                    crate.setInventorySlotContents(-1, new ItemStack(Items.apple));
                    fail("Crate should have throw an exception for setting negative slot");
                } catch (RuntimeException e)
                {
                    //This should happen
                    if (!e.getMessage().contains(crate.toString()))
                    {
                        e.printStackTrace();
                        fail("Error didn't contain tile");
                    }
                }
                try
                {
                    crate.setInventorySlotContents(crate.getSizeInventory(), new ItemStack(Items.apple));
                    fail("Crate should have throw an exception for setting negative slot");
                } catch (RuntimeException e)
                {
                    //This should happen
                    if (!e.getMessage().contains(crate.toString()))
                    {
                        e.printStackTrace();
                        fail("Error didn't contain tile");
                    }
                }
            }
        }
    }

    /** Tests {@link TileCrate#canInsertItem(int, ItemStack, int)} */
    public void testCanInsert()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);

        //Check sanity
        assertTrue("Inventory should be empty", crate.inventory.isEmpty());
        assertTrue("Current stack should be null", crate.currentItem == null);
        assertTrue("Current stack size should be zero", crate.currentStackSize == 0);

        //Check input options
        assertFalse("Can't insert null", crate.canInsertItem(0, null, 0));
        assertTrue("We should be able to insert an apple", crate.canInsertItem(0, new ItemStack(Items.apple), 0));
        assertFalse("Can't insert into a negative slot", crate.canInsertItem(-1, new ItemStack(Items.apple), 0));
        assertFalse("Can't insert into a slot above max", crate.canInsertItem(crate.getSizeInventory(), new ItemStack(Items.apple), 0));
    }

    /** Tests {@link TileCrate#canExtractItem(int, ItemStack, int)} */
    public void testCanExtract()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);

        assertFalse("Can't insert into a negative slot", crate.canExtractItem(-1, new ItemStack(Items.apple), 0));
        assertFalse("Can't insert into a slot above max", crate.canExtractItem(crate.getSizeInventory(), new ItemStack(Items.apple), 0));
    }

    /** Tests {@link TileCrate#getAccessibleSlotsFromSide(int)} */
    public void testAccessibleSides()
    {
        for (TileCrate.CrateType type : TileCrate.CrateType.values())
        {
            TileCrate crate = new TileCrate();
            crate.crateType = type;
            crate.setWorldObj(world);
            //valid inputs, normal opteration only 0 -> 5 should be passed in
            for (int i = 0; i < 6; i++)
            {
                assertTrue(crate.getAccessibleSlotsFromSide(i) == type.slots);
            }
            //Invalid inputs, this should never be called but is handled just in case of people don't know the API
            assertTrue(crate.getAccessibleSlotsFromSide(-1) == TileEntityBase.EMPTY_INT_ARRAY);
            assertTrue(crate.getAccessibleSlotsFromSide(6) == TileEntityBase.EMPTY_INT_ARRAY);
        }
    }

    /** Tests {@link TileCrate#doesItemStackMatch(ItemStack)} */
    public void testDoesItemStackMatch()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        assertTrue("Current item should init null", crate.currentItem == null);

        //Test null
        assertTrue("Current item should match null", crate.doesItemStackMatch(null));
        assertFalse("Current item should not match apple", crate.doesItemStackMatch(new ItemStack(Items.apple)));

        //Test non-null match
        crate.currentItem = new ItemStack(Items.apple);
        assertTrue("Current item should match own value", crate.doesItemStackMatch(crate.currentItem));
        assertTrue("Current item should match apple", crate.doesItemStackMatch(new ItemStack(Items.apple)));
        //Test invalid inputs for match
        assertFalse("Current item should not match null", crate.doesItemStackMatch(null));
        assertFalse("Current item should not match stick", crate.doesItemStackMatch(new ItemStack(Items.stick)));
        assertFalse("Current item should not match apple with meta", crate.doesItemStackMatch(new ItemStack(Items.apple, 1, 1)));
        ItemStack t1 = new ItemStack(Items.apple);
        t1.setTagCompound(new NBTTagCompound());
        t1.getTagCompound().setBoolean("someValue", true);
        assertFalse("Current item should not match apple with nbt", crate.doesItemStackMatch(t1));

        //TODO maybe do more test matching for current item with meta, nbt, and both
    }

    /** Tests {@link TileCrate#isValid(ItemStack)} */
    public void testIsValid()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);

        assertFalse("Null should not be valid", crate.isValid(null));
        assertTrue("Apple should be valid", crate.isValid(new ItemStack(Items.apple)));
    }

    /** Tests {@link TileCrate#isItemValidForSlot(int, ItemStack)} */
    public void testIsItemValidForSlot()
    {
        for (TileCrate.CrateType type : TileCrate.CrateType.values())
        {
            TileCrate crate = new TileCrate();
            crate.crateType = type;
            crate.setWorldObj(world);
            for (int slot = 0; slot < crate.getSizeInventory(); slot++)
            {
                assertTrue("Apple should be valid for slot " + slot, crate.isItemValidForSlot(slot, new ItemStack(Items.apple)));
                assertFalse("Null should be invalid for slot" + slot, crate.isItemValidForSlot(slot, null));
                crate.currentItem = new ItemStack(Items.stone_hoe);
                assertFalse("Apple should be invalid for slot " + slot, crate.isItemValidForSlot(slot, new ItemStack(Items.apple)));
                assertTrue("Stone hoe should be valid for slot " + slot, crate.isItemValidForSlot(slot, new ItemStack(Items.stone_hoe)));
                crate.currentItem = null;
            }
            //Invalid inputs, this should never be called but is handled just in case of people don't know the API
            assertFalse(crate.isItemValidForSlot(-1, new ItemStack(Items.apple)));
            assertFalse(crate.isItemValidForSlot(-1, null));
            assertFalse(crate.isItemValidForSlot(crate.getSizeInventory(), new ItemStack(Items.apple)));
            assertFalse(crate.isItemValidForSlot(crate.getSizeInventory(), null));
        }
    }

    /** Tests {@link TileCrate#getInventoryStackLimit()} */
    public void testGetInventoryStackLimit()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        assertTrue("Inventory stack limit should be 64 by default", crate.getInventoryStackLimit() == 64);
        crate.currentItem = new ItemStack(Items.apple);
        assertTrue("Inventory stack limit should be 64 by default", crate.getInventoryStackLimit() == Items.apple.getItemStackLimit());
        crate.currentItem = new ItemStack(Items.iron_door);
        assertTrue("Inventory stack limit should be 64 by default", crate.getInventoryStackLimit() == Items.iron_door.getItemStackLimit());
    }

    /** Tests {@link TileCrate#isUseableByPlayer(EntityPlayer)} */
    public void testIsUseableByPlayer()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        assertTrue("Player should be able to access the tile for zero distance", crate.isUseableByPlayer(player));
        player.setLocationAndAngles(0, 6, 0, 0, 0);
        assertFalse(crate.isUseableByPlayer(player));
    }

    /** Tests {@link TileCrate#decrStackSize(int, int)} */
    public void testDecrStackSize()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        assertTrue("Slot 0 should be empty", crate.getStackInSlot(0) == null);
        assertTrue("Should return null as slot is empty", crate.decrStackSize(0, 1) == null);
        crate.setInventorySlotContents(0, new ItemStack(Items.apple, 64));

        //Test a partial consume of slot
        ItemStack stack = crate.decrStackSize(0, 1);
        assertTrue("Should have returned an apple", areItemStacksEqual(stack, new ItemStack(Items.apple)));
        assertTrue("Should have returned an one apple", stack.stackSize == 1);
        stack = crate.getStackInSlot(0);
        assertTrue("Slot 0 should still contain apples", areItemStacksEqual(stack, new ItemStack(Items.apple)));
        assertTrue("Slot 0 should contain 63 apples", stack.stackSize == 63);

        //Test full consume of slot
        crate.setInventorySlotContents(0, new ItemStack(Items.apple, 64));
        stack = crate.decrStackSize(0, 64);
        assertTrue("Should have returned an apple", areItemStacksEqual(stack, new ItemStack(Items.apple)));
        assertTrue("Should have returned an one apple", stack.stackSize == 64);
        assertTrue("Slot 0 should be empty", crate.getStackInSlot(0) == null);

        //Test full consume of slot
        crate.setInventorySlotContents(0, new ItemStack(Items.apple, 64));
        stack = crate.decrStackSize(0, -1);
        assertTrue("Slot split stack should be null", stack == null);
        assertTrue("Slot 0 should be contain apples", areItemStacksEqual(crate.getStackInSlot(0), new ItemStack(Items.apple)));
        assertTrue("Slot 0 should be contain 64 apples", crate.getStackInSlot(0).stackSize == 64);
    }

    /** Tests {@link TileCrate#clearInventory()} */
    public void testClearInventory()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        crate.currentItem = new ItemStack(Items.record_11);
        crate.currentStackSize = 10;
        crate.inventory.put(1, new ItemStack(Items.record_13));
        crate.clearInventory();
        assertTrue(crate.currentItem == null);
        assertTrue(crate.currentStackSize == 0);
        assertTrue(crate.inventory.isEmpty());
    }

    /** Tests {@link TileCrate#getNextEmptySlot()} */
    public void testGetNextEmptySlot()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        for (int i = 0; i < crate.getSizeInventory(); i++)
        {
            int slot = crate.getNextEmptySlot();
            int estimatedSlotsLeft = crate.getSizeInventory() - i;
            assertTrue("slotsWithRoomStack should contain " + estimatedSlotsLeft + " slots but contained " + crate.slotsWithRoomStack.size(), crate.slotsWithRoomStack.size() == estimatedSlotsLeft);
            assertTrue("Next empty slot should equal current slot " + i + ", instead it equals " + slot, slot == i);
            crate.setInventorySlotContents(i, new ItemStack(Items.apple, 64));
        }
        //Fill slot
        crate.clearInventory();
        crate.setInventorySlotContents(20, new ItemStack(Items.apple, 64));
        assertTrue("Slot 20 should contain apples", areItemStacksEqual(crate.getStackInSlot(20), new ItemStack(Items.apple)));
        assertTrue("Next empty slot should be 0 even though slot 20 is full", crate.getNextEmptySlot() == 0);
        assertFalse("Slot stack should not contain 20", crate.slotsWithRoomStack.contains(20));
        assertTrue("Should be " + (crate.getSizeInventory() - 1) + " slots left but there is " + crate.slotsWithRoomStack.size(), (crate.getSizeInventory() - 1) == crate.slotsWithRoomStack.size());

        //Set item but don't fill
        crate.clearInventory();
        crate.setInventorySlotContents(20, new ItemStack(Items.apple, 1));
        assertTrue("Next empty slot should be 0 even though slot 20 is set", crate.getNextEmptySlot() == 0);
        assertTrue("Should be " + crate.getSizeInventory() + " slots left but there is " + crate.slotsWithRoomStack.size(), crate.getSizeInventory() == crate.slotsWithRoomStack.size());

    }

    /** Tests {@link TileCrate#rebuildEntireInventory()} */
    public void testRebuildEntireInventory()
    {
        TileCrate crate = new TileCrate();
        crate.setWorldObj(world);
        ItemStack stack;

        //Test call if current stack is null
        crate.rebuildEntireInventory();
        for (int i = 0; i < crate.getSizeInventory(); i++)
        {
            assertTrue("Slot " + i + " should empty", crate.getStackInSlot(i) == null);
        }
        crate.clearInventory();

        //Test call if stack size is zero
        crate.currentItem = new ItemStack(Items.apple);
        crate.rebuildEntireInventory();
        for (int i = 0; i < crate.getSizeInventory(); i++)
        {
            assertTrue("Slot " + i + " should empty", crate.getStackInSlot(i) == null);
        }
        assertTrue("Current item should have been cleared due to zero stack size", crate.currentItem == null);
        crate.clearInventory();

        //Test generic fill inventory
        crate.currentItem = new ItemStack(Items.apple);
        crate.currentStackSize = (crate.getInventoryStackLimit() * 10) + 10;
        crate.rebuildEntireInventory();
        assertTrue("Inventory should have 11 slots with content", crate.inventory.size() == 11);
        int slot = crate.getNextEmptySlot();
        assertTrue("Next empty slot should be 10 but is " + slot, slot == 10);
        assertTrue("Should be " + (crate.getSizeInventory() - 10) + " slots with space but list contains " + crate.slotsWithRoomStack.size(), (crate.getSizeInventory() - 10) == crate.slotsWithRoomStack.size());
        for (int i = 0; i < 10; i++)
        {
            stack = crate.getStackInSlot(i);
            assertTrue("Slot " + i + " should contain apples", areItemStacksEqual(stack, new ItemStack(Items.apple)));
            assertTrue("Slot " + i + " should contain 64 apples", stack.stackSize == crate.getInventoryStackLimit());
        }
        stack = crate.getStackInSlot(10);
        assertTrue("Slot 10 should contain apples", areItemStacksEqual(stack, new ItemStack(Items.apple)));
        assertTrue("Slot 10 should contain apples 10 apples", stack.stackSize == 10);
        for (int i = 11; i < crate.getSizeInventory(); i++)
        {
            assertTrue("Slot " + i + " should empty", crate.getStackInSlot(i) == null);
        }
        crate.clearInventory();

        //Test compete fill
        crate.currentItem = new ItemStack(Items.apple);
        crate.currentStackSize = crate.getSizeInventory() * crate.getInventoryStackLimit();
        crate.rebuildEntireInventory();
        assertTrue("Inventory should have " + crate.getSizeInventory() + " slots with content", crate.inventory.size() == crate.getSizeInventory());
        assertTrue("Next empty slot should be -2", crate.getNextEmptySlot() == -2);
        assertTrue("Slots with space should be empty", crate.slotsWithRoomStack.isEmpty());
        for (int i = 0; i < crate.getSizeInventory(); i++)
        {
            stack = crate.getStackInSlot(i);
            assertTrue("Slot " + i + " should contain apples", areItemStacksEqual(stack, new ItemStack(Items.apple)));
            assertTrue("Slot " + i + " should contain 64 apples", stack.stackSize == crate.getInventoryStackLimit());
        }
        crate.clearInventory();

        //Test over fill
        crate.currentItem = new ItemStack(Items.apple);
        crate.currentStackSize = crate.getSizeInventory() * crate.getInventoryStackLimit() * 10; //yes this is extreme but why not
        crate.rebuildEntireInventory();
        assertTrue("Inventory should have " + crate.getSizeInventory() + " slots with content", crate.inventory.size() == crate.getSizeInventory());
        assertTrue("Next empty slot should be -2", crate.getNextEmptySlot() == -2);
        assertTrue("Slots with space should be empty", crate.slotsWithRoomStack.isEmpty());
        for (int i = 0; i < crate.getSizeInventory(); i++)
        {
            stack = crate.getStackInSlot(i);
            assertTrue("Slot " + i + " should contain apples", areItemStacksEqual(stack, new ItemStack(Items.apple)));
            assertTrue("Slot " + i + " should contain 64 apples", stack.stackSize == crate.getInventoryStackLimit());
        }
        crate.clearInventory();
    }

    /** Tests {@link TileCrate#increaseCount(int)} */
    public void testIncreaseCount()
    {

    }

    /** Tests {@link TileCrate#decreaseCount(int)} */
    public void testDecreaseCount()
    {

    }

    /** Tests {@link TileCrate#onPlayerRightClick(EntityPlayer, int, Pos)} */
    public void testOnPlayerRightClick()
    {

    }

    /**
     * Simple method to compare two ItemStacks using the ItemStackWrapper
     * rather than using Vanilla MC's methods which assume stacksize. ItemStack
     * data and order does matter when comparing items. As the item wrapper will
     * ignore meta and NBT if the stack provided doesn't use them. So ensure
     * your first stack has the most data on it.
     *
     * @param a - first stack
     * @param b - second stack
     * @return true if they match ID, Meta, and NBT
     */
    private static boolean areItemStacksEqual(ItemStack a, ItemStack b)
    {
        if (a == null && b == null)
        {
            return true;
        }
        else if (a != null && b != null)
        {
            return new ItemStackWrapper(a).equals(new ItemStackWrapper(b));
        }
        return false;
    }
}
