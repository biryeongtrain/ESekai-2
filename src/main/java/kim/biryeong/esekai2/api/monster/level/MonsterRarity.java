package kim.biryeong.esekai2.api.monster.level;

import com.mojang.serialization.Codec;

/**
 * Minimal monster rarity axis used by current monster level scaling and item level derivation.
 */
public enum MonsterRarity {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE;

    public static final Codec<MonsterRarity> CODEC = Codec.STRING.xmap(
            value -> MonsterRarity.valueOf(value.toUpperCase()),
            value -> value.name().toLowerCase()
    );
}
