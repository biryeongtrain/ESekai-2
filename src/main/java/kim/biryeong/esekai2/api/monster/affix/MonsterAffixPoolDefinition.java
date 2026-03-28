package kim.biryeong.esekai2.api.monster.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kim.biryeong.esekai2.api.monster.level.MonsterLevelContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Objects;

/**
 * Describes the monster affix candidate pool for one monster entity type.
 *
 * @param entityType target vanilla entity type
 * @param defaultSpawnContext default level and rarity context used until world-driven context selection exists
 * @param candidateAffixIds stable affix ids eligible to roll for this monster type
 */
public record MonsterAffixPoolDefinition(
        EntityType<?> entityType,
        MonsterLevelContext defaultSpawnContext,
        List<Identifier> candidateAffixIds
) {
    private static final Codec<MonsterAffixPoolDefinition> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(MonsterAffixPoolDefinition::entityType),
            MonsterLevelContext.CODEC.fieldOf("default_spawn_context").forGetter(MonsterAffixPoolDefinition::defaultSpawnContext),
            Codec.list(Identifier.CODEC).fieldOf("candidate_affix_ids").forGetter(MonsterAffixPoolDefinition::candidateAffixIds)
    ).apply(instance, MonsterAffixPoolDefinition::new));

    /**
     * Validated codec used to decode monster affix pools from datapacks.
     */
    public static final Codec<MonsterAffixPoolDefinition> CODEC = BASE_CODEC.validate(MonsterAffixPoolDefinition::validate);

    public MonsterAffixPoolDefinition {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(defaultSpawnContext, "defaultSpawnContext");
        Objects.requireNonNull(candidateAffixIds, "candidateAffixIds");

        candidateAffixIds = List.copyOf(candidateAffixIds);
        for (Identifier candidateAffixId : candidateAffixIds) {
            Objects.requireNonNull(candidateAffixId, "candidateAffixIds entry");
        }
    }

    private static DataResult<MonsterAffixPoolDefinition> validate(MonsterAffixPoolDefinition definition) {
        if (definition.candidateAffixIds().isEmpty()) {
            return DataResult.error(() -> "candidate_affix_ids must not be empty");
        }

        return DataResult.success(definition);
    }
}
