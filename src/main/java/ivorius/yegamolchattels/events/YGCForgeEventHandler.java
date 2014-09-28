/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.yegamolchattels.events;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ivorius.ivtoolkit.rendering.IvRenderHelper;
import ivorius.yegamolchattels.achievements.YGCAchievementList;
import ivorius.yegamolchattels.blocks.TileEntityMicroBlock;
import ivorius.yegamolchattels.blocks.TileEntitySnowGlobe;
import ivorius.yegamolchattels.blocks.TileEntityStatue;
import ivorius.yegamolchattels.client.rendering.EntityBlockTextureMerger;
import ivorius.yegamolchattels.entities.EntityGhost;
import ivorius.yegamolchattels.items.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

/**
 * Created by lukas on 24.05.14.
 */
public class YGCForgeEventHandler
{
    public void register()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event)
    {
        EntityLivingBase renderEntity = Minecraft.getMinecraft().renderViewEntity;
        ItemStack heldItem = renderEntity.getHeldItem();
        if (heldItem != null && (heldItem.getItem() instanceof MicroblockSelector))
        {
            MicroblockSelector selector = (MicroblockSelector) heldItem.getItem();
            if (selector.showMicroblockSelection(renderEntity, heldItem))
                renderSelectedMicroblock(event.partialTicks, selector.microblockSelectionSize(heldItem));
        }
    }

    @SideOnly(Side.CLIENT)
    public static void renderSelectedMicroblock(float partialTicks, float size)
    {
        EntityLivingBase renderEntity = Minecraft.getMinecraft().renderViewEntity;
        MovingObjectPosition hoveredObject = Minecraft.getMinecraft().objectMouseOver;
        ItemChisel.MicroBlockFragment hoveredFragment = ItemChisel.getHoveredFragment(renderEntity, hoveredObject.blockX, hoveredObject.blockY, hoveredObject.blockZ);

        if (hoveredFragment != null)
        {
            double viewerPosX = renderEntity.lastTickPosX + (renderEntity.posX - renderEntity.lastTickPosX) * (double) partialTicks;
            double viewerPosY = renderEntity.lastTickPosY + (renderEntity.posY - renderEntity.lastTickPosY) * (double) partialTicks;
            double viewerPosZ = renderEntity.lastTickPosZ + (renderEntity.posZ - renderEntity.lastTickPosZ) * (double) partialTicks;

            GL11.glPushMatrix();
            GL11.glTranslated(hoveredFragment.getCoord().x - viewerPosX, hoveredFragment.getCoord().y - viewerPosY, hoveredFragment.getCoord().z - viewerPosZ);

            GL11.glTranslated((hoveredFragment.getInternalCoord().x + 0.5f) / TileEntityMicroBlock.MICROBLOCKS_PER_BLOCK_X, (hoveredFragment.getInternalCoord().y + 0.5f) / TileEntityMicroBlock.MICROBLOCKS_PER_BLOCK_Y, (hoveredFragment.getInternalCoord().z + 0.5f) / TileEntityMicroBlock.MICROBLOCKS_PER_BLOCK_Z);
            GL11.glColor3f(0.0f, 0.0f, 0.0f);
            GL11.glLineWidth(1.0f);
            IvRenderHelper.drawCuboid(Tessellator.instance, size / TileEntityMicroBlock.MICROBLOCKS_PER_BLOCK_X, size / TileEntityMicroBlock.MICROBLOCKS_PER_BLOCK_Y, size / TileEntityMicroBlock.MICROBLOCKS_PER_BLOCK_Z, 1.0f, true);

            GL11.glPopMatrix();
        }
    }

    @SubscribeEvent
    public void unloadWorld(WorldEvent.Unload event)
    {
        if (event.world.isRemote)
            unloadAllGLObjects(event.world.loadedTileEntityList);
    }

    @SubscribeEvent
    public void unloadChunk(ChunkEvent.Unload event)
    {
        if (event.world.isRemote)
            unloadAllGLObjects(event.getChunk().chunkTileEntityMap.values());
    }

    @SideOnly(Side.CLIENT)
    private void unloadAllGLObjects(Collection tileEntities)
    {
        for (Object tileEntity : tileEntities)
        {
            if (tileEntity instanceof TileEntitySnowGlobe)
            {
                ((TileEntitySnowGlobe) tileEntity).addCallListForDestruction();
            }
            else if (tileEntity instanceof TileEntityStatue)
            {
                ((TileEntityStatue) tileEntity).releaseTexture();
            }
        }
    }

    @SubscribeEvent
    public void blockHarvested(BlockEvent.HarvestDropsEvent event)
    {
        if (event.harvester != null)
        {
            ItemStack equipped = event.harvester.getCurrentEquippedItem();
            if (equipped != null && equipped.getItem() instanceof ItemClubHammer)
            {
                ((ItemClubHammer) equipped.getItem()).modifyDrops(event.world, event.block, equipped, event.x, event.y, event.z, event.drops);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event)
    {
        if (event.entityLiving instanceof EntityGhost)
        {
            Entity sourceEntity = event.source.getEntity();
            if (sourceEntity instanceof EntityPlayer)
            {
                ((EntityPlayer) sourceEntity).triggerAchievement(YGCAchievementList.ghostKilled);
            }
        }
    }

//    @SubscribeEvent
//    public void onLivingDrops(LivingDropsEvent event)
//    {
//        if (event.entityLiving.getRNG().nextFloat() < 0.5f)
//        {
//            ItemStack vitaStack = ItemEntityVita.createVitaItemStackAsNewbornEntity(YGCItems.entityVita, event.entityLiving);
//            event.drops.add(new EntityItem(event.entity.worldObj, event.entity.posX, event.entity.posY, event.entity.posZ, vitaStack));
//       }
//    }

    @SubscribeEvent
    public void textureStitch(TextureStitchEvent event)
    {
        if (event instanceof TextureStitchEvent.Pre)
        {
            EntityBlockTextureMerger.clearCachedStitchedTexture();
        }
    }
}
