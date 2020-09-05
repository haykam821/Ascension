package io.github.haykam821.ascension.game.phase;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.ascension.game.AscensionConfig;
import io.github.haykam821.ascension.game.map.AscensionMap;
import io.github.haykam821.ascension.game.map.AscensionMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

public class AscensionWaitingPhase {
	private final GameWorld gameWorld;
	private final AscensionMap map;
	private final AscensionConfig config;

	public AscensionWaitingPhase(GameWorld gameWorld, AscensionMap map, AscensionConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<GameWorld> open(GameOpenContext<AscensionConfig> context) {
		AscensionMapBuilder mapBuilder = new AscensionMapBuilder(context.getConfig());

		return mapBuilder.create().thenCompose(map -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
				.setGenerator(map.createGenerator(context.getServer()))
				.setDefaultGameMode(GameMode.ADVENTURE);

			return context.openWorld(worldConfig).thenApply(gameWorld -> {
				AscensionWaitingPhase phase = new AscensionWaitingPhase(gameWorld, map, context.getConfig());

				return GameWaitingLobby.open(gameWorld, context.getConfig().getPlayerConfig(), game -> {
					AscensionActivePhase.setRules(game);

					// Listeners
					game.on(PlayerAddListener.EVENT, phase::addPlayer);
					game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
					game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
					game.on(RequestStartListener.EVENT, phase::requestStart);
				});
			});
		});
	}

	private boolean isFull() {
		return this.gameWorld.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameWorld.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		AscensionActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.OK;
	}

	public void addPlayer(ServerPlayerEntity player) {
		AscensionActivePhase.spawn(this.gameWorld.getWorld(), this.map, player);
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		AscensionActivePhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return ActionResult.SUCCESS;
	}
}