package com.builtbroken.boxofcrates.content.chest;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.network.IPacketIDReceiver;
import com.builtbroken.mc.core.network.packet.PacketTile;
import com.builtbroken.mc.core.network.packet.PacketType;
import com.builtbroken.mc.prefab.items.ItemStackWrapper;
import com.builtbroken.mc.prefab.tile.entity.TileEntityInv;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class TileChest extends TileEntityInv implements IPacketIDReceiver
{
    //TODO have custom name render on outside of chest
    /** Determines if the check for adjacent chests has taken place. */
    public boolean adjacentChestChecked;
    /** Contains the chest tile located adjacent to this one (if any) */
    public TileChest adjacentChestZNeg;
    /** Contains the chest tile located adjacent to this one (if any) */
    public TileChest adjacentChestXPos;
    /** Contains the chest tile located adjacent to this one (if any) */
    public TileChest adjacentChestXNeg;
    /** Contains the chest tile located adjacent to this one (if any) */
    public TileChest adjacentChestZPos;
    /** The current angle of the lid (between 0 and 1) */
    public float lidAngle;
    /** The angle of the lid last tick */
    public float prevLidAngle;
    /** The number of players currently using this chest */
    public int numPlayersUsing;

    /** Server sync counter (once per 20 ticks) */
    private int ticksSinceSync;
    public String customName = "";

    private HashMap<ForgeDirection, List<ItemStackWrapper>> filterBySideMap = new HashMap();
    private HashMap<Integer, ItemStackWrapper> filterBySlot = new HashMap();

    public void addFilterForSide(ForgeDirection side, ItemStack stack)
    {
        if (stack != null)
        {
            List<ItemStackWrapper> list = null;
            if (filterBySideMap.containsKey(side))
            {
                list = filterBySideMap.get(side);
            }

            if (list == null)
            {
                list = new ArrayList();
            }
            ItemStackWrapper wrapper = new ItemStackWrapper(stack);
            if (!list.contains(wrapper))
            {
                list.add(wrapper);
                filterBySideMap.put(side, list);
                if (!worldObj.isRemote)
                {
                    Engine.instance.packetHandler.sendToAllAround(new PacketTile(this, 1, (byte) side.ordinal(), stack), this);
                }
            }
        }
    }

    public void removeFilterForSide(ForgeDirection side, ItemStack stack)
    {
        if (stack != null && filterBySideMap.containsKey(side))
        {
            List<ItemStackWrapper> list = filterBySideMap.get(side);
            if (list != null && list.contains(stack))
            {
                list.remove(stack);
                if (!worldObj.isRemote)
                    Engine.instance.packetHandler.sendToAllAround(new PacketTile(this, 2, (byte) side.ordinal(), stack), this);
                if (!list.isEmpty())
                {
                    filterBySideMap.put(side, list);
                }
                else
                {
                    filterBySideMap.remove(side);
                }
            }
        }
    }

    public void setFilterForSlot(int slot, ItemStack stack)
    {
        if (stack == null)
        {
            if (filterBySideMap.containsKey(slot))
            {
                filterBySlot.remove(slot);
                if (!worldObj.isRemote)
                {
                    Engine.instance.packetHandler.sendToAllAround(new PacketTile(this, 3, slot, stack), this);
                }
            }
        }
        else
        {
            filterBySlot.put(slot, new ItemStackWrapper(stack));
            if (!worldObj.isRemote)
            {
                Engine.instance.packetHandler.sendToAllAround(new PacketTile(this, 4, slot), this);
            }
        }
    }

    @Override
    public boolean read(ByteBuf buf, int id, EntityPlayer player, PacketType type)
    {
        if (worldObj.isRemote)
        {
            //Desc packet
            if (id == 0)
            {
                return true;
            }
            //Gui Update packet
            else if (id == 5)
            {
                return true;
            }
            //Add filter to side
            else if (id == 1)
            {
                addFilterForSide(ForgeDirection.getOrientation(buf.readByte()), ByteBufUtils.readItemStack(buf));
                return true;
            }
            //Remove filter from side
            else if (id == 2)
            {
                removeFilterForSide(ForgeDirection.getOrientation(buf.readByte()), ByteBufUtils.readItemStack(buf));
                return true;
            }
            //Set filter for slot, stack
            else if (id == 3)
            {
                setFilterForSlot(buf.readInt(), ByteBufUtils.readItemStack(buf));
                return true;
            }
            //Set filter for slot, null
            else if (id == 4)
            {
                setFilterForSlot(buf.readInt(), null);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getInventoryName()
    {
        return this.hasCustomInventoryName() ? this.customName : "container.chest.filtered";
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return this.customName != null && this.customName.length() > 0;
    }

    public void setCustomName(String name)
    {
        this.customName = name;
        //if (!getWorldObj().isRemote)
        //PacketManager.sendToAllAround(new PacketChest(this, PacketChest.ChestPacketType.NAME), this);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        if (nbt.hasKey("CustomName", 8))
        {
            this.customName = nbt.getString("CustomName");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        if (this.hasCustomInventoryName())
        {
            nbt.setString("CustomName", this.customName);
        }
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        this.adjacentChestChecked = false;
    }

    private void checkAdjacentChest(TileChest tile, int side)
    {
        if (tile.isInvalid())
        {
            this.adjacentChestChecked = false;
        }
        else if (this.adjacentChestChecked)
        {
            switch (side)
            {
                case 0:
                    if (this.adjacentChestZPos != tile)
                    {
                        this.adjacentChestChecked = false;
                    }

                    break;
                case 1:
                    if (this.adjacentChestXNeg != tile)
                    {
                        this.adjacentChestChecked = false;
                    }

                    break;
                case 2:
                    if (this.adjacentChestZNeg != tile)
                    {
                        this.adjacentChestChecked = false;
                    }

                    break;
                case 3:
                    if (this.adjacentChestXPos != tile)
                    {
                        this.adjacentChestChecked = false;
                    }
            }
        }
    }

    /**
     * Performs the check for adjacent chests to determine if this chest is double or not.
     */
    public void checkForAdjacentChests()
    {
        if (!this.adjacentChestChecked)
        {
            this.adjacentChestChecked = true;
            this.adjacentChestZNeg = null;
            this.adjacentChestXPos = null;
            this.adjacentChestXNeg = null;
            this.adjacentChestZPos = null;

            if (this.canConnectToBlock(this.xCoord - 1, this.yCoord, this.zCoord))
            {
                this.adjacentChestXNeg = (TileChest) this.getWorldObj().getTileEntity(this.xCoord - 1, this.yCoord, this.zCoord);
            }

            if (this.canConnectToBlock(this.xCoord + 1, this.yCoord, this.zCoord))
            {
                this.adjacentChestXPos = (TileChest) this.getWorldObj().getTileEntity(this.xCoord + 1, this.yCoord, this.zCoord);
            }

            if (this.canConnectToBlock(this.xCoord, this.yCoord, this.zCoord - 1))
            {
                this.adjacentChestZNeg = (TileChest) this.getWorldObj().getTileEntity(this.xCoord, this.yCoord, this.zCoord - 1);
            }

            if (this.canConnectToBlock(this.xCoord, this.yCoord, this.zCoord + 1))
            {
                this.adjacentChestZPos = (TileChest) this.getWorldObj().getTileEntity(this.xCoord, this.yCoord, this.zCoord + 1);
            }

            if (this.adjacentChestZNeg != null)
            {
                this.adjacentChestZNeg.checkAdjacentChest(this, 0);
            }

            if (this.adjacentChestZPos != null)
            {
                this.adjacentChestZPos.checkAdjacentChest(this, 2);
            }

            if (this.adjacentChestXPos != null)
            {
                this.adjacentChestXPos.checkAdjacentChest(this, 1);
            }

            if (this.adjacentChestXNeg != null)
            {
                this.adjacentChestXNeg.checkAdjacentChest(this, 3);
            }
        }
    }

    private boolean canConnectToBlock(int x, int y, int z)
    {
        if (this.getWorldObj() != null)
        {
            TileEntity tile = this.getWorldObj().getTileEntity(x, y, z);
            return tile instanceof TileChest;
        }
        return false;
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        this.checkForAdjacentChests();
        ++this.ticksSinceSync;
        float f;

        if (!this.getWorldObj().isRemote && this.numPlayersUsing != 0 && (this.ticksSinceSync + this.xCoord + this.yCoord + this.zCoord) % 200 == 0)
        {
            this.numPlayersUsing = 0;
            f = 5.0F;
            List list = this.getWorldObj().getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox((double) ((float) this.xCoord - f), (double) ((float) this.yCoord - f), (double) ((float) this.zCoord - f), (double) ((float) (this.xCoord + 1) + f), (double) ((float) (this.yCoord + 1) + f), (double) ((float) (this.zCoord + 1) + f)));
            Iterator iterator = list.iterator();

            while (iterator.hasNext())
            {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                if (entityplayer.openContainer instanceof ContainerChest)
                {
                    IInventory iinventory = ((ContainerChest) entityplayer.openContainer).getLowerChestInventory();

                    if (iinventory == this || iinventory instanceof InventoryLargeChest && ((InventoryLargeChest) iinventory).isPartOfLargeChest(this))
                    {
                        ++this.numPlayersUsing;
                    }
                }
            }
        }

        this.prevLidAngle = this.lidAngle;
        f = 0.1F;
        double d2;

        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0F && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null)
        {
            double d1 = (double) this.xCoord + 0.5D;
            d2 = (double) this.zCoord + 0.5D;

            if (this.adjacentChestZPos != null)
            {
                d2 += 0.5D;
            }

            if (this.adjacentChestXPos != null)
            {
                d1 += 0.5D;
            }

            this.getWorldObj().playSoundEffect(d1, (double) this.yCoord + 0.5D, d2, "random.chestopen", 0.5F, this.getWorldObj().rand.nextFloat() * 0.1F + 0.9F);
        }

        if (this.numPlayersUsing == 0 && this.lidAngle > 0.0F || this.numPlayersUsing > 0 && this.lidAngle < 1.0F)
        {
            float f1 = this.lidAngle;

            if (this.numPlayersUsing > 0)
            {
                this.lidAngle += f;
            }
            else
            {
                this.lidAngle -= f;
            }

            if (this.lidAngle > 1.0F)
            {
                this.lidAngle = 1.0F;
            }

            float f2 = 0.5F;

            if (this.lidAngle < f2 && f1 >= f2 && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null)
            {
                d2 = (double) this.xCoord + 0.5D;
                double d0 = (double) this.zCoord + 0.5D;

                if (this.adjacentChestZPos != null)
                {
                    d0 += 0.5D;
                }

                if (this.adjacentChestXPos != null)
                {
                    d2 += 0.5D;
                }

                this.getWorldObj().playSoundEffect(d2, (double) this.yCoord + 0.5D, d0, "random.chestclosed", 0.5F, this.getWorldObj().rand.nextFloat() * 0.1F + 0.9F);
            }

            if (this.lidAngle < 0.0F)
            {
                this.lidAngle = 0.0F;
            }
        }
    }

    @Override
    public boolean receiveClientEvent(int packet_id, int value)
    {
        if (packet_id == 1)
        {
            this.numPlayersUsing = value;
            return true;
        }
        else
        {
            return super.receiveClientEvent(packet_id, value);
        }
    }

    @Override
    public void openInventory()
    {
        if (this.numPlayersUsing < 0)
        {
            this.numPlayersUsing = 0;
        }

        ++this.numPlayersUsing;
        this.getWorldObj().addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(), 1, this.numPlayersUsing);
        this.getWorldObj().notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
        this.getWorldObj().notifyBlocksOfNeighborChange(this.xCoord, this.yCoord - 1, this.zCoord, this.getBlockType());
    }

    @Override
    public void closeInventory()
    {
        if (this.getBlockType() instanceof BlockChest)
        {
            --this.numPlayersUsing;
            this.getWorldObj().addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(), 1, this.numPlayersUsing);
            this.getWorldObj().notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
            this.getWorldObj().notifyBlocksOfNeighborChange(this.xCoord, this.yCoord - 1, this.zCoord, this.getBlockType());
        }
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        this.updateContainingBlockInfo();
        this.checkForAdjacentChests();
    }
}