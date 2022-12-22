package io.github.haykam821.ascension.game.phase;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.ascension.game.AscensionConfig;
import io.github.haykam821.ascension.game.map.AscensionMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class AscensionActivePhase {
	private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final AscensionMap map;
	private final AscensionConfig config;
	private final Set<ServerPlayerEntity> players;
	private int ticksElapsed = 0;

	public AscensionActivePhase(GameSpace gameSpace, ServerWorld world, AscensionMap map, AscensionConfig config, Set<ServerPlayerEntity> players) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
		this.players = players;
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, AscensionMap map, AscensionConfig config) {
		AscensionActivePhase phase = new AscensionActivePhase(gameSpace, world, map, config, Sets.newHashSet(gameSpace.getPlayers()));

		gameSpace.setActivity(activity -> {
			AscensionActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
		});
	}

	public void enable() {
 		for (ServerPlayerEntity player : this.players) {
			player.changeGameMode(GameMode.ADVENTURE);
		
			StatusEffectInstance jumpBoost = new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20000000, this.config.getJumpBoostAmplifier(), true, false, false);
			player.addStatusEffect(jumpBoost);
		
			AscensionActivePhase.spawn(this.world, this.map, player);
		}
	}

	private int getEndY() {
		return this.map.getBounds().max().getY();
	}

	private boolean isFinished(ServerPlayerEntity player) {
		return player.getY() > this.getEndY();
	}

	private Text getWinMessage(ServerPlayerEntity player) {
		String time = AscensionActivePhase.FORMAT.format(this.ticksElapsed / (double) 20);
		return player.getDisplayName().copy().append(" has won the game in " + time + " seconds!").formatted(Formatting.GOLD);
	}

	private void tick() {
		int minY = this.map.getBounds().min().getY();

		for (ServerPlayerEntity player : this.players) {
			if (this.isFinished(player)) {
				// Send win message
				Text message = this.getWinMessage(player);
				this.gameSpace.getPlayers().sendMessage(message);

				this.gameSpace.close(GameCloseReason.FINISHED);
				return;
			}
			
			if (player.getY() < minY) {
				AscensionActivePhase.spawn(this.world, this.map, player);
			}

			player.experienceProgress = (float) MathHelper.clamp(player.getY() / this.getEndY(), 0, 1);
			player.setExperienceLevel((int) Math.max(0, this.getEndY() - player.getY()));
		}

		this.ticksElapsed += 1;
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	public PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, AscensionActivePhase.getSpawnPos(this.world, this.map)).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.players.remove(player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		AscensionActivePhase.spawn(this.world, this.map, player);
		return ActionResult.SUCCESS;
	}

	public static Vec3d getSpawnPos(ServerWorld world, AscensionMap map) {
		BlockPos min = map.getBounds().min();
		BlockPos max = map.getBounds().max();

		int x = world.getRandom().nextInt(max.getX() - min.getX()) + min.getX();
		int z = world.getRandom().nextInt(max.getZ() - min.getZ()) + min.getZ();

		return new Vec3d(x + 0.5, 1, z + 0.5);
	}

	public static void spawn(ServerWorld world, AscensionMap map, ServerPlayerEntity player) {
		Vec3d spawnPos = AscensionActivePhase.getSpawnPos(world, map);
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
	}

	static {
		FORMAT.setRoundingMode(RoundingMode.DOWN);
	}
}
