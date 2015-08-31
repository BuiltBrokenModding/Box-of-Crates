package com.builtbroken.boxofcrates;

import com.builtbroken.boxofcrates.chest.BlockChest;
import com.builtbroken.boxofcrates.chest.ItemBlockChest;
import com.builtbroken.boxofcrates.chest.TileChest;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = BoxOfCrates.DOMAIN, name = "Box of Crates", version = "@MAJOR@.@MINOR@.@REVIS@.@BUILD@")
public class BoxOfCrates
{
    public static final String DOMAIN = "boxofcrates";
    public static final String PREFIX = DOMAIN + ":";

    @SidedProxy(clientSide = "com.builtbroken.boxofcrates.ClientProxy", serverSide = "com.builtbroken.boxofcrates.CommonProxy")
    public static CommonProxy proxy;

    public static Logger LOGGER;

    public static Block blockChest;


    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        LOGGER = LogManager.getLogger("BoxOfCrates");

        blockChest = new BlockChest();
        GameRegistry.registerBlock(blockChest, ItemBlockChest.class, "filteredChest");
        GameRegistry.registerTileEntity(TileChest.class, "filteredChest");

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        GameRegistry.addShapedRecipe(new ItemStack(blockChest), "rgr", "ece", "rgr", 'c', Blocks.chest, 'r', Items.redstone, 'g', Items.gold_nugget);
        proxy.postInit();
    }
}
