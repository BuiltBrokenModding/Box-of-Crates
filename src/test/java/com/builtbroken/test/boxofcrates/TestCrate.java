package com.builtbroken.test.boxofcrates;

import com.builtbroken.boxofcrates.content.crate.TileCrate;
import com.builtbroken.mc.testing.junit.AbstractTest;
import com.builtbroken.mc.testing.junit.VoltzTestRunner;
import com.builtbroken.mc.testing.junit.world.FakeWorldServer;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.util.FakePlayer;
import org.junit.runner.RunWith;

/** JUnit test for {@link com.builtbroken.boxofcrates.content.crate.TileCrate}
 * Created by Dark on 9/3/2015.
 */
@RunWith(VoltzTestRunner.class)
public class TestCrate extends AbstractTest
{
    private static FakeWorldServer world;
    private static FakePlayer player;

    @Override
    public void setUpForEntireClass()
    {
        world = FakeWorldServer.newWorld("TestCrate");
        player = new FakePlayer(world, new GameProfile(null, "CrateTester"));
    }

    public void testInit()
    {
        TileCrate crate = new TileCrate();
    }

}
