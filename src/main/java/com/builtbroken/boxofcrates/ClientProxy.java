package com.builtbroken.boxofcrates;

import com.builtbroken.boxofcrates.content.chest.ItemChestRender;
import com.builtbroken.boxofcrates.content.chest.RenderChest;
import com.builtbroken.boxofcrates.content.chest.TileChest;
import cpw.mods.fml.client.registry.ClientRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

/**
 * Created by Dark on 7/25/2015.
 */
public class ClientProxy extends CommonProxy
{
    @Override
    public void init()
    {
        super.init();
        ClientRegistry.bindTileEntitySpecialRenderer(TileChest.class, new RenderChest());
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(BoxOfCrates.blockFilteredChest), new ItemChestRender());
    }
}
