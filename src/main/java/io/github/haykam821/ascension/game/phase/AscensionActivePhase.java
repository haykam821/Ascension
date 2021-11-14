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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;

public class AscensionActivePhase {
	private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final AscensionMap map;
	private final AscensionConfig config;
	private final Set<ServerPlayerEntity> players;
	private int ticksElapsed = 0;

	public AscensionActivePhase(GameSpace gameSpace, AscensionMap map, AscensionConfig config, Set<ServerPlayerEntity> players) {
		this.gameSpace = gameSpace;
		this.world = gameSpace.getWorld();
		this.map = map;
		this.config = config;
		this.players = players;
	}

	public static void setRules(GameLogic game) {
		game.deny(GameRule.CRAFTING);
		game.deny(GameRule.FALL_DAMAGE);
		game.deny(GameRule.HUNGER);
		game.deny(GameRule.PORTALS);
		game.deny(GameRule.PVP);
	}

	public static void open(GameSpace gameSpace, AscensionMap map, AscensionConfig config) {
		AscensionActivePhase phase = new AscensionActivePhase(gameSpace, map, config, Sets.newHashSet(gameSpace.getPlayers()));

		gameSpace.openGame(game -> {
			AscensionActivePhase.setRules(game);

			// Listeners
			game.listen(GameOpenListener.EVENT, phase::open);
			game.listen(GameTickListener.EVENT, phase::tick);
			game.listen(PlayerAddListener.EVENT, phase::addPlayer);
			game.listen(PlayerDeathListener.EVENT, phase::onPlayerDeath);
		});
	}

	public void open() {
 		for (ServerPlayerEntity player : this.players) {
			player.setGameMode(GameMode.ADVENTURE);
		
			StatusEffectInstance jumpBoost = new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20000000, this.config.getJumpBoostAmplifier(), true, false, false);
			player.addStatusEffect(jumpBoost);
		
			AscensionActivePhase.spawn(this.world, this.map, player);
		}
	}

	private int getEndY() {
		return this.map.getBounds().getMax().getY();
	}

	private boolean isFinished(ServerPlayerEntity player) {
		return player.getY() > this.getEndY();
	}

	private Text getWinMessage(ServerPlayerEntity player) {
		String time = AscensionActivePhase.FORMAT.format(this.ticksElapsed / (double) 20);
		return player.getDisplayName().shallowCopy().append(" has won the game in " + time + " seconds!").formatted(Formatting.GOLD);
	}

	private void tick() {
		int minY = this.map.getBounds().getMin().getY();

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

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	private void addPlayer(PlayerEntity player) {
		if (!this.players.contains(player)) {
			this.setSpectator(player);
		}
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		AscensionActivePhase.spawn(this.world, this.map, player);
		return ActionResult.SUCCESS;
	}

	public static void spawn(ServerWorld world, AscensionMap map, ServerPlayerEntity player) {
		BlockPos min = map.getBounds().getMin();
		BlockPos max = map.getBounds().getMax();

		int x = world.getRandom().nextInt(max.getX() - min.getX()) + min.getX();
		int z = world.getRandom().nextInt(max.getZ() - min.getZ()) + min.getZ();

		player.teleport(world, x, 1, z, 0, 0);
	}

	static {
		FORMAT.setRoundingMode(RoundingMode.DOWN);
	}
}
