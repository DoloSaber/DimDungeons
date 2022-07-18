package com.catastrophe573.dimdungeons.utils;

import java.util.Collection;
import java.util.Collections;

import com.catastrophe573.dimdungeons.item.ItemPortalKey;
import com.catastrophe573.dimdungeons.item.ItemRegistrar;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CommandDimDungeons
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
	// register all commands "under" a single cheat "/dimdungeons". This is a more polite way of doing things. (/givekey interferes with /give anyway)
	
	// the current cheat structures are:
	// /dimdungeons givekey [player] [string type] [int theme, optional]
	// /dimdungeons getpersonal [player]
	
	// the first half of the /givekey cheat
	LiteralArgumentBuilder<CommandSourceStack> argumentBuilder = Commands.literal("dimdungeons").requires((cmd) ->
	{
	    return cmd.hasPermission(2);
	});

	// make a different cheat for "givekey basic", "givekey advanced", etc
	String[] keytypes = { "blank", "basic", "advanced" };
	for (int i = 0; i < keytypes.length; i++)
	{
	    String type = keytypes[i];
	    argumentBuilder.then(Commands.literal("givekey").then(Commands.literal(type).executes((cmd) ->
	    {
		return giveKey(cmd, Collections.singleton(cmd.getSource().getPlayerOrException()), type, 0);
	    }).then(Commands.argument("target", EntityArgument.players()).executes((cmd) ->
	    {
		return giveKey(cmd, EntityArgument.getPlayers(cmd, "target"), type, 0);
	    }).then(Commands.argument("theme", IntegerArgumentType.integer(0)).executes((cmd) ->
	    {
		return giveKey(cmd, EntityArgument.getPlayers(cmd, "target"), type, IntegerArgumentType.getInteger(cmd, "theme"));
	    })))));
	}

	// register the /givekey cheat
	dispatcher.register(argumentBuilder);
    }

    private static int giveKey(CommandContext<CommandSourceStack> cmd, Collection<ServerPlayer> targets, String type, int theme) throws CommandSyntaxException
    {
	MutableComponent keyName = Component.translatable("item.dimdungeons.item_portal_key"); // for use with the logging at the end of the function

	for (ServerPlayer serverplayerentity : targets)
	{
	    // make a new and different key for each player
	    ItemStack stack = new ItemStack(ItemRegistrar.ITEM_PORTAL_KEY.get());

	    // which type of key was requested
	    if ("blank".equals(type))
	    {
		keyName = Component.translatable("item.dimdungeons.item_portal_key");
	    }
	    else if ("basic".equals(type))
	    {
		((ItemPortalKey) (ItemRegistrar.ITEM_PORTAL_KEY.get())).activateKeyLevel1(cmd.getSource().getServer(), stack, theme);
		keyName = Component.translatable("item.dimdungeons.item_portal_key_basic");
	    }
	    else if ("advanced".equals(type))
	    {
		((ItemPortalKey) (ItemRegistrar.ITEM_PORTAL_KEY.get())).activateKeyLevel2(cmd.getSource().getServer(), stack);
		keyName = Component.translatable("item.dimdungeons.item_portal_key_advanced");
	    }
	    else
	    {
		// unreachable code as long as register and this else-if chain are kept in sync
	    }

	    // try to give the player the item
	    boolean flag = serverplayerentity.getInventory().add(stack);

	    // if that fails then throw it on the ground at the player's feet
	    if (flag && stack.isEmpty())
	    {
		stack.setCount(1);
		ItemEntity itementity = serverplayerentity.drop(stack, false);
		if (itementity != null)
		{
		    itementity.makeFakeItem();
		}

		serverplayerentity.level.playSound((Player) null, serverplayerentity.getX(), serverplayerentity.getY(), serverplayerentity.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((serverplayerentity.getRandom().nextFloat() - serverplayerentity.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
		serverplayerentity.inventoryMenu.broadcastChanges();
	    }
	    else
	    {
		// keys don't normally stack, but just in case this block of code gives a stack of keys
		ItemEntity itementity = serverplayerentity.drop(stack, false);
		if (itementity != null)
		{
		    itementity.setNoPickUpDelay();
		    itementity.setOwner(serverplayerentity.getUUID());
		}
	    }
	}

	// print either "Gave one [key] to Dev" or "Gave one [key] to X players"
	if (targets.size() == 1)
	{
	    cmd.getSource().sendSuccess(Component.translatable("commands.give.success.single", 1, keyName, targets.iterator().next().getDisplayName()), true);
	}
	else
	{
	    cmd.getSource().sendSuccess(Component.translatable("commands.give.success.single", 1, keyName, targets.size()), true);
	}

	return targets.size();
    }
}