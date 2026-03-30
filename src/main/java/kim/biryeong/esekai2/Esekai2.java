package kim.biryeong.esekai2;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import kim.biryeong.esekai2.impl.ailment.AilmentBootstrap;
import kim.biryeong.esekai2.impl.config.monster.MonsterAffixConfigManager;
import kim.biryeong.esekai2.impl.item.affix.AffixBootstrap;
import kim.biryeong.esekai2.impl.item.level.ItemLevelBootstrap;
import kim.biryeong.esekai2.impl.level.LevelBootstrap;
import kim.biryeong.esekai2.impl.monster.affix.MonsterAffixBootstrap;
import kim.biryeong.esekai2.impl.monster.level.MonsterLevelBootstrap;
import kim.biryeong.esekai2.impl.monster.stat.MonsterStatBootstrap;
import kim.biryeong.esekai2.impl.skill.entity.SkillEntityBootstrap;
import kim.biryeong.esekai2.impl.skill.registry.SkillBootstrap;
import kim.biryeong.esekai2.impl.stat.registry.StatBootstrap;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Esekai2 implements ModInitializer {
    public static final String MOD_ID = "esekai2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override
    public void onInitialize() {
        LOGGER.info("Hello! Welcome to ESekai!");
        MonsterAffixConfigManager.bootstrap();
        StatBootstrap.bootstrap();
        LevelBootstrap.bootstrap();
        AffixBootstrap.bootstrap();
        ItemLevelBootstrap.bootstrap();
        MonsterAffixBootstrap.bootstrap();
        AilmentBootstrap.bootstrap();
        SkillBootstrap.bootstrap();
        SkillEntityBootstrap.bootstrap();
        MonsterLevelBootstrap.bootstrap();
        MonsterStatBootstrap.bootstrap();
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        PolymerResourcePackUtils.markAsRequired();
    }
}
