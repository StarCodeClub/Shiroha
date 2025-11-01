package i.moniasuki.shiroha.config.vanilla;

import io.papermc.paper.configuration.GlobalConfiguration;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

import java.util.Map;

@ConfigClassInfo(name = "mc_technical_survival_mode", comments = "Technical Survival Mode settings", category = EnumConfigCategory.FUNCTION)
public class MCTechnicalSurvivalModeConfig implements IConfigModule {
    @ConfigInfo(name = "enabled", comments = "Enable this function")
    public static boolean enabled = false;

    public static void doMcTechnicalModeIf() {
        if (enabled) {
            doMcTechnicalMode();
        }
    }

    public static void doMcTechnicalMode() {
        GlobalConfiguration.get().unsupportedSettings.allowPistonDuplication = true;
        GlobalConfiguration.get().unsupportedSettings.allowHeadlessPistons = true;
        GlobalConfiguration.get().unsupportedSettings.allowPermanentBlockBreakExploits = true;
        GlobalConfiguration.get().unsupportedSettings.allowUnsafeEndPortalTeleportation = true;
        GlobalConfiguration.get().unsupportedSettings.skipTripwireHookPlacementValidation = true;
        GlobalConfiguration.get().packetLimiter.allPackets = new GlobalConfiguration.PacketLimiter.PacketLimit(GlobalConfiguration.get().packetLimiter.allPackets.interval(),
                5000.0, GlobalConfiguration.get().packetLimiter.allPackets.action());
        GlobalConfiguration.get().packetLimiter.overrides = Map.of();
        GlobalConfiguration.get().itemValidation.resolveSelectorsInBooks = true;
        GlobalConfiguration.get().scoreboards.saveEmptyScoreboardTeams = true;
    }
}
