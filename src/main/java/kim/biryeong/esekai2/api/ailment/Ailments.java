package kim.biryeong.esekai2.api.ailment;

import kim.biryeong.esekai2.impl.ailment.AilmentBootstrap;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Public entry points for ailment runtime state attached to live entities.
 */
public final class Ailments {
    private Ailments() {
    }

    /**
     * Ensures ailment effect and attachment bootstrapping has run.
     */
    public static void bootstrap() {
        AilmentBootstrap.bootstrap();
    }

    /**
     * Returns the currently attached ailment state for one entity.
     *
     * @param entity queried live entity
     * @return ailment state when present
     */
    public static Optional<AilmentState> get(LivingEntity entity) {
        return AilmentBootstrap.get(entity);
    }
}
