package com.builtbroken.boxofcrates;

import com.builtbroken.boxofcrates.content.chest.BlockChest;
import com.builtbroken.boxofcrates.content.chest.ItemBlockChest;
import com.builtbroken.boxofcrates.content.chest.TileChest;
import com.builtbroken.mc.lib.mod.AbstractMod;
import com.builtbroken.mc.lib.mod.AbstractProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

@Mod(modid = BoxOfCrates.DOMAIN, name = "Box of Crates", version = "@MAJOR@.@MINOR@.@REVIS@.@BUILD@")
public class BoxOfCrates extends AbstractMod
{
    public static final String DOMAIN = "boxofcrates";
    public static final String PREFIX = DOMAIN + ":";

    @SidedProxy(clientSide = "com.builtbroken.boxofcrates.ClientProxy", serverSide = "com.builtbroken.boxofcrates.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance(DOMAIN)
    public static BoxOfCrates INSTANCE;

    public static Block blockFilteredChest;

    public BoxOfCrates()
    {
        super(DOMAIN);
    }


    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);

        blockFilteredChest = new BlockChest();
        GameRegistry.registerBlock(blockFilteredChest, ItemBlockChest.class, "filteredChest");
        GameRegistry.registerTileEntity(TileChest.class, "filteredChest");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        super.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        super.postInit(event);
        GameRegistry.addShapedRecipe(new ItemStack(blockFilteredChest), "rgr", "ece", "rgr", 'c', Blocks.chest, 'r', Items.redstone, 'g', Items.gold_nugget);
    }

    @Override
    public AbstractProxy getProxy()
    {
        return proxy;
    }
}
