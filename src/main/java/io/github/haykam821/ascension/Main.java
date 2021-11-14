package io.github.haykam821.ascension;

import io.github.haykam821.ascension.game.AscensionConfig;
import io.github.haykam821.ascension.game.phase.AscensionWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "ascension";

	private static final Identifier ASCENSION_ID = new Identifier(MOD_ID, "ascension");
	public static final GameType<AscensionConfig> ASCENSION_TYPE = GameType.register(ASCENSION_ID, AscensionConfig.CODEC, AscensionWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}
}