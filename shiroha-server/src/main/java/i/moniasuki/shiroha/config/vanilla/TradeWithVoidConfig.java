package i.moniasuki.shiroha.config.vanilla;

import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(name = "trade_with_void", category = EnumConfigCategory.FIXES)
public class TradeWithVoidConfig {
    @ConfigInfo(name = "enabled")
    public static boolean enabled = false;
}
