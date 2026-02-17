package moe.starcraft.shiroha.config.functions;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(name = "leaves_modify_config", category = EnumConfigCategory.FUNCTION)
public class LeavesModifyConfig implements IConfigModule {
    @ConfigInfo(name = "no_chat_sign")
    public static boolean noChatSign = false;
}
