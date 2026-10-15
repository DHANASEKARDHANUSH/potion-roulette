package potion_roulette;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Iterator;
import java.util.Locale;

public class PotionRoulette implements ModInitializer {

    public static final String MOD_ID = "potion-roulette";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    // Minecraft runs at approximately 20 ticks per second.
    private static final int EFFECT_INTERVAL_TICKS = 20 * 60;

    private static final Random RANDOM = new Random();

    // Stores the time remaining for each player participating.
    private static final Map<UUID, Integer> ACTIVE_PLAYERS =
            new HashMap<>();
    // Tracks each player's diamond count during the challenge.
	private static final Map<UUID, Integer> LAST_DIAMOND_COUNTS =
        	new HashMap<>();
    private static final int TITLE_FADE_IN_TICKS = 10;
    private static final int TITLE_FADE_OUT_TICKS = 10;
    private static final int CHALLENGE_TITLE_STAY_TICKS = 60;
    private static final int FIRST_EFFECT_TITLE_DELAY_TICKS =
            TITLE_FADE_IN_TICKS + CHALLENGE_TITLE_STAY_TICKS + TITLE_FADE_OUT_TICKS;

    // Show the first effect result after the challenge title has finished displaying.
    private static final Map<UUID, PendingTitle> PENDING_EFFECT_TITLES =
            new HashMap<>();

    private void showTitle(
            ServerPlayer player,
            String title,
            String subtitle,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                fadeInTicks,
                stayTicks,
                fadeOutTicks
        ));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal(subtitle)
        ));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(title)
        ));
    }
    // Effects that Diamond Roulette can select.
    private static final List<EffectChoice> EFFECTS = List.of(
            new EffectChoice("Speed", MobEffects.SPEED, false),
            new EffectChoice("Strength", MobEffects.STRENGTH, false),
            new EffectChoice("Regeneration", MobEffects.REGENERATION, false),
            new EffectChoice("Haste", MobEffects.HASTE, false),

            new EffectChoice("Slowness", MobEffects.SLOWNESS, true),
            new EffectChoice("Weakness", MobEffects.WEAKNESS, true),
            new EffectChoice("Poison", MobEffects.POISON, true),
            new EffectChoice("Mining Fatigue", MobEffects.MINING_FATIGUE, true)
    );

    @Override
    public void onInitialize() {

        LOGGER.info("Diamond Roulette is loading!");

        // Register /potionroulettestart.
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {

                    dispatcher.register(
                            Commands.literal("potionroulettestart")
                                    .executes(context -> {

                                        ServerPlayer player =
                                                context.getSource()
                                                        .getPlayerOrException();

                                        return startRoulette(player);
                                    })
                    );
                }
        );

        // Run the countdown on the server.
        ServerTickEvents.END_SERVER_TICK.register(
                this::onServerTick
        );
    }

    private int startRoulette(ServerPlayer player) {

        // This challenge is designed for Survival mode.
        if (!player.gameMode.getGameModeForPlayer().isSurvival()) {

            showTitle(
                    player,
                    "SURVIVAL ONLY",
                    "Switch to Survival mode!",
                    TITLE_FADE_IN_TICKS,
                    60,
                    TITLE_FADE_OUT_TICKS
            );

            return 0;
        }

        UUID playerId = player.getUUID();

        // Prevent accidentally starting the same challenge twice.
        if (ACTIVE_PLAYERS.containsKey(playerId)) {

            showTitle(
                    player,
                    "ALREADY RUNNING",
                    "Your challenge is active!",
                    TITLE_FADE_IN_TICKS,
                    60,
                    TITLE_FADE_OUT_TICKS
            );

            return 0;
        }

        // Remember the player's diamond count when the challenge starts.
        LAST_DIAMOND_COUNTS.put(
        	playerId,
        	player.getInventory().countItem(Items.DIAMOND)
		);

		// Start this player's 60-second countdown.
		ACTIVE_PLAYERS.put(playerId, EFFECT_INTERVAL_TICKS);

        showTitle(
                player,
                "DIAMOND ROULETTE",
                "The challenge has begun!",
                TITLE_FADE_IN_TICKS,
                CHALLENGE_TITLE_STAY_TICKS,
                TITLE_FADE_OUT_TICKS
        );

        // Give the first effect immediately.
        giveRandomEffect(player, true);

        LOGGER.info(
                "Diamond Roulette started for {}",
                player.getName().getString()
        );

        return 1;
    }

    private void onServerTick(MinecraftServer server) {

    Iterator<Map.Entry<UUID, Integer>> iterator =
            ACTIVE_PLAYERS.entrySet().iterator();

    while (iterator.hasNext()) {

        Map.Entry<UUID, Integer> entry = iterator.next();

        UUID playerId = entry.getKey();

        ServerPlayer player =
                server.getPlayerList().getPlayer(playerId);

        // Stop tracking disconnected players.
        if (player == null) {
            LAST_DIAMOND_COUNTS.remove(playerId);
            PENDING_EFFECT_TITLES.remove(playerId);
            iterator.remove();
            continue;
        }

        // Count the diamonds currently in the player's inventory.
        int currentDiamonds =
                player.getInventory().countItem(Items.DIAMOND);

        // The challenge requires Survival mode.
        // Ignore inventory changes made outside Survival.
        if (!player.gameMode.getGameModeForPlayer().isSurvival()) {
            LAST_DIAMOND_COUNTS.put(playerId, currentDiamonds);
            continue;
        }

        int previousDiamonds =
                LAST_DIAMOND_COUNTS.getOrDefault(
                        playerId,
                        currentDiamonds
                );

        // WIN CONDITION: The player has obtained a diamond.
        if (currentDiamonds > previousDiamonds) {

            showTitle(
                    player,
                    "VICTORY!",
                    "You obtained a diamond!",
                    TITLE_FADE_IN_TICKS,
                    100,
                    TITLE_FADE_OUT_TICKS
            );

            LOGGER.info(
                    "{} completed Diamond Roulette!",
                    player.getName().getString()
            );

            // End the challenge for this player.
            LAST_DIAMOND_COUNTS.remove(playerId);
            PENDING_EFFECT_TITLES.remove(playerId);
            iterator.remove();

            continue;
        }

        // Remember the latest inventory count.
        LAST_DIAMOND_COUNTS.put(playerId, currentDiamonds);

        PendingTitle pendingTitle = PENDING_EFFECT_TITLES.get(playerId);
        if (pendingTitle != null && pendingTitle.ticksRemaining() <= 1) {
            showTitle(
                    player,
                    pendingTitle.title(),
                    pendingTitle.subtitle(),
                    TITLE_FADE_IN_TICKS,
                    50,
                    TITLE_FADE_OUT_TICKS
            );
            PENDING_EFFECT_TITLES.remove(playerId);
        } else if (pendingTitle != null) {
            PENDING_EFFECT_TITLES.put(
                    playerId,
                    new PendingTitle(
                            pendingTitle.title(),
                            pendingTitle.subtitle(),
                            pendingTitle.ticksRemaining() - 1
                    )
            );
        }

        // Process the existing 60-second effect timer.
        int remainingTicks = entry.getValue() - 1;

        if (remainingTicks <= 0) {

            giveRandomEffect(player);

            // Begin the next countdown.
            entry.setValue(EFFECT_INTERVAL_TICKS);

        } else {

            entry.setValue(remainingTicks);
        }
    }
}

    private void giveRandomEffect(ServerPlayer player) {
        giveRandomEffect(player, false);
    }

    private void giveRandomEffect(ServerPlayer player, boolean delayNotification) {

        EffectChoice choice =
                EFFECTS.get(RANDOM.nextInt(EFFECTS.size()));

        MobEffectInstance effect = new MobEffectInstance(
        choice.effect(),
        MobEffectInstance.INFINITE_DURATION,
        0
);

        player.addEffect(effect);

        String title = choice.name().toUpperCase(Locale.ROOT);
        String subtitle = choice.negative() ? "BAD LUCK!" : "GOOD LUCK!";

        if (delayNotification) {
            PENDING_EFFECT_TITLES.put(
                    player.getUUID(),
                    new PendingTitle(title, subtitle, FIRST_EFFECT_TITLE_DELAY_TICKS)
            );
        } else {
            showTitle(
                    player,
                    title,
                    subtitle,
                    TITLE_FADE_IN_TICKS,
                    50,
                    TITLE_FADE_OUT_TICKS
            );
        }

        LOGGER.info(
                "{} received effect {}",
                player.getName().getString(),
                choice.name()
        );
    }

    private record EffectChoice(
            String name,
            Holder<MobEffect> effect,
            boolean negative
    ) {}

    private record PendingTitle(
            String title,
            String subtitle,
            int ticksRemaining
    ) {}
}
