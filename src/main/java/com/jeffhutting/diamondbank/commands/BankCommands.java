package com.jeffhutting.diamondbank.commands;

import com.jeffhutting.diamondbank.data.BankState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BankCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("bank")

                    // /bank balance
                    .then(literal("balance")
                        .executes(ctx -> executeBalance(ctx.getSource())))

                    // /bank deposit <amount>
                    .then(literal("deposit")
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> executeDeposit(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "amount")
                            ))))

                    // /bank withdraw <amount>
                    .then(literal("withdraw")
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> executeWithdraw(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "amount")
                            ))))

                    // /bank pay <player> <amount>
                    .then(literal("pay")
                        .then(argument("player", EntityArgument.player())
                            .then(argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> executePay(
                                    ctx.getSource(),
                                    EntityArgument.getPlayer(ctx, "player"),
                                    IntegerArgumentType.getInteger(ctx, "amount")
                                )))))

                    // /bank admin — requires operator permission (level 2)
                    .then(literal("admin")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))

                        // /bank admin give <player> <amount>
                        .then(literal("give")
                            .then(argument("player", EntityArgument.player())
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                    .executes(ctx -> executeAdminGive(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "amount")
                                    )))))

                        // /bank admin set <player> <amount>
                        .then(literal("set")
                            .then(argument("player", EntityArgument.player())
                                .then(argument("amount", IntegerArgumentType.integer(0))
                                    .executes(ctx -> executeAdminSet(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "amount")
                                    ))))))
            );
        });
    }

    // -----------------------------------------------------------------------
    // /bank balance — show your current balance
    // -----------------------------------------------------------------------
    private static int executeBalance(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        BankState state = BankState.getServerState(source.getServer());
        long balance = state.getBalance(player.getUUID());

        source.sendSuccess(
            () -> Component.literal("💎 Your bank balance: " + balance + " diamond(s)"),
            false
        );
        return 1;
    }

    // -----------------------------------------------------------------------
    // /bank deposit <amount> — take diamonds from inventory, add to balance
    // -----------------------------------------------------------------------
    private static int executeDeposit(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        int found = countDiamonds(player);
        if (found < amount) {
            source.sendFailure(Component.literal(
                "You don't have enough diamonds. You have " + found + " diamond(s)."
            ));
            return 0;
        }

        removeDiamonds(player, amount);
        BankState state = BankState.getServerState(source.getServer());
        state.deposit(player.getUUID(), amount);

        source.sendSuccess(
            () -> Component.literal("💎 Deposited " + amount + " diamond(s). Balance: "
                + state.getBalance(player.getUUID())),
            false
        );
        return 1;
    }

    // -----------------------------------------------------------------------
    // /bank withdraw <amount> — remove from balance, give diamonds to inventory
    // -----------------------------------------------------------------------
    private static int executeWithdraw(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        BankState state = BankState.getServerState(source.getServer());

        if (!state.withdraw(player.getUUID(), amount)) {
            source.sendFailure(Component.literal(
                "Insufficient balance. You have " + state.getBalance(player.getUUID()) + " diamond(s)."
            ));
            return 0;
        }

        giveDiamonds(player, amount);

        source.sendSuccess(
            () -> Component.literal("💎 Withdrew " + amount + " diamond(s). Balance: "
                + state.getBalance(player.getUUID())),
            false
        );
        return 1;
    }

    // -----------------------------------------------------------------------
    // /bank pay <player> <amount> — transfer balance to another player
    // -----------------------------------------------------------------------
    private static int executePay(CommandSourceStack source, ServerPlayer target, int amount) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        if (target.getUUID().equals(sender.getUUID())) {
            source.sendFailure(Component.literal("You can't pay yourself."));
            return 0;
        }

        BankState state = BankState.getServerState(source.getServer());

        if (!state.withdraw(sender.getUUID(), amount)) {
            source.sendFailure(Component.literal(
                "Insufficient balance. You have " + state.getBalance(sender.getUUID()) + " diamond(s)."
            ));
            return 0;
        }

        state.deposit(target.getUUID(), amount);

        sender.sendSystemMessage(Component.literal(
            "💎 Sent " + amount + " diamond(s) to " + target.getName().getString()
            + ". Your balance: " + state.getBalance(sender.getUUID())
        ));
        target.sendSystemMessage(Component.literal(
            "💎 " + sender.getName().getString() + " sent you " + amount + " diamond(s)."
            + " Your balance: " + state.getBalance(target.getUUID())
        ));

        return 1;
    }

    // -----------------------------------------------------------------------
    // /bank admin give <player> <amount> — add to balance without diamonds
    // -----------------------------------------------------------------------
    private static int executeAdminGive(CommandSourceStack source, ServerPlayer target, int amount) {
        BankState state = BankState.getServerState(source.getServer());
        state.deposit(target.getUUID(), amount);

        source.sendSuccess(
            () -> Component.literal("💎 Gave " + amount + " diamond(s) to "
                + target.getName().getString()
                + ". Their balance: " + state.getBalance(target.getUUID())),
            true
        );
        target.sendSystemMessage(Component.literal(
            "💎 An admin credited " + amount + " diamond(s) to your bank account."
        ));
        return 1;
    }

    // -----------------------------------------------------------------------
    // /bank admin set <player> <amount> — set balance to exact amount
    // -----------------------------------------------------------------------
    private static int executeAdminSet(CommandSourceStack source, ServerPlayer target, int amount) {
        BankState state = BankState.getServerState(source.getServer());
        state.setBalance(target.getUUID(), amount);

        source.sendSuccess(
            () -> Component.literal("💎 Set " + target.getName().getString()
                + "'s balance to " + amount + " diamond(s)."),
            true
        );
        return 1;
    }

    // -----------------------------------------------------------------------
    // INVENTORY HELPERS
    // -----------------------------------------------------------------------

    // Count how many diamonds a player has across all 36 main inventory slots.
    private static int countDiamonds(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.DIAMOND) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // Remove a specific number of diamonds from a player's inventory.
    private static void removeDiamonds(ServerPlayer player, int amount) {
        int remaining = amount;
        for (int i = 0; i < 36; i++) {
            if (remaining <= 0) break;
            var stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.DIAMOND) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    // Give a specific number of diamonds to a player's inventory.
    // Diamonds stack up to 64 — give them in stacks to fill slots efficiently.
    private static void giveDiamonds(ServerPlayer player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, 64);
            var stack = Items.DIAMOND.getDefaultInstance();
            stack.setCount(stackSize);
            player.getInventory().add(stack);
            remaining -= stackSize;
        }
    }
}
