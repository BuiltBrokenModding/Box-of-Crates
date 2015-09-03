package com.builtbroken.test.boxofcrates;

import com.builtbroken.boxofcrates.content.crate.TileCrate;
import com.builtbroken.mc.prefab.items.ItemStackWrapper;
import com.builtbroken.mc.testing.junit.AbstractTest;
import com.builtbroken.mc.testing.junit.VoltzTestRunner;
import com.builtbroken.mc.testing.junit.server.FakeDedicatedServer;
import com.builtbroken.mc.testing.junit.testers.TestPlayer;
import com.builtbroken.mc.testing.junit.world.FakeWorldServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
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
    private static FakeWorldServer world;
    private static EntityPlayer player;

    @Override
    public void setUpForEntireClass()
    {
        MinecraftServer server = new FakeDedicatedServer(new File(FakeWorldServer.baseFolder, "CrateTester"));
        world = FakeWorldServer.newWorld(server, "TestCrate");
        player = new TestPlayer(server, world, new GameProfile(null, "CrateTester"));
    }

    public void testInit()
    {
        TileCrate crate = new TileCrate();
    }

    public void testItemStackEqual()
    {
        assertTrue(areItemStacksEqual(new ItemStack(Items.apple), new ItemStack(Items.apple)));
        assertFalse(areItemStacksEqual(new ItemStack(Items.apple), new ItemStack(Items.stick)));
    }

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
            }
        }
    }

    public static boolean areItemStacksEqual(ItemStack a, ItemStack b)
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
