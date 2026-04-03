package kim.biryeong.esekai2.impl.skill.particle;

import de.tomalbrc.sandstorm.Particles;
import kim.biryeong.esekai2.Esekai2;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads bundled Sandstorm particle definitions that ESekai skill datapacks reference.
 */
public final class SandstormParticleBootstrap {
    private static final String[] BUNDLED_PARTICLES = {
            "/particle/basic_strike_burst.json",
            "/particle/arcane_burst_pulse.json",
            "/particle/bleed_strike_burst.json",
            "/particle/stun_strike_burst.json",
            "/particle/frost_strike_burst.json",
            "/particle/freeze_strike_burst.json",
            "/particle/ember_strike_burst.json",
            "/particle/shock_strike_burst.json",
            "/particle/toxic_strike_burst.json",
            "/particle/fireball_cast_burst.json",
            "/particle/fireball_trail_burst.json",
            "/particle/focus_guard_aura.json",
            "/particle/focus_burst_pulse.json",
            "/particle/charged_focus_aura.json",
            "/particle/cleanse_wave_ring.json",
            "/particle/resource_charge_aura.json",
            "/particle/restorative_pulse_ring.json",
            "/particle/barrier_guard_aura.json"
    };

    private SandstormParticleBootstrap() {
    }

    public static void bootstrap() {
        for (String resourcePath : BUNDLED_PARTICLES) {
            try (InputStream inputStream = SandstormParticleBootstrap.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    Esekai2.LOGGER.warn("Missing bundled Sandstorm particle resource {}", resourcePath);
                    continue;
                }

                Particles.loadEffect(inputStream);
            } catch (IOException exception) {
                Esekai2.LOGGER.error("Failed to load bundled Sandstorm particle resource {}", resourcePath, exception);
            } catch (RuntimeException exception) {
                Esekai2.LOGGER.error("Failed to parse bundled Sandstorm particle resource {}", resourcePath, exception);
            }
        }
    }
}
