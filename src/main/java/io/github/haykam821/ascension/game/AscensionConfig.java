package io.github.haykam821.ascension.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.ascension.game.map.AscensionMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class AscensionConfig {
	public static final Codec<AscensionConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			AscensionMapConfig.CODEC.fieldOf("map").forGetter(AscensionConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(AscensionConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("jump_boost_amplifier", 8).forGetter(AscensionConfig::getJumpBoostAmplifier)
		).apply(instance, AscensionConfig::new);
	});

	private final AscensionMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int jumpBoostAmplifier;

	public AscensionConfig(AscensionMapConfig mapConfig, PlayerConfig playerConfig, int jumpBoostAmplifier) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.jumpBoostAmplifier = jumpBoostAmplifier;
	}

	public AscensionMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getJumpBoostAmplifier() {
		return this.jumpBoostAmplifier;
	}
}