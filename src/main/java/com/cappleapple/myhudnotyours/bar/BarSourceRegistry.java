package com.cappleapple.myhudnotyours.bar;

import com.cappleapple.myhudnotyours.compat.irons_spellbooks.IronsSpellbooksManaSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.neoforged.fml.ModList;

/** Internal semantic-source registry; compatibility modules can register here later. */
public final class BarSourceRegistry {
    private static final Map<String, NumericBarSource> SOURCES = new LinkedHashMap<>();
    private static boolean optionalIntegrationsInitialized;

    static {
        register(simple("minecraft:health", "Health", minecraft -> {
            LocalPlayer player = minecraft.player;
            return player == null ? inactive("Health")
                    : active(player.getHealth(), 0.0, player.getMaxHealth(), "Health");
        }));
        register(simple("minecraft:absorption", "Absorption", minecraft -> {
            LocalPlayer player = minecraft.player;
            double value = player == null ? 0.0 : player.getAbsorptionAmount();
            return player == null ? inactive("Absorption") : new NumericBarSnapshot(value, 0.0,
                    Math.max(1.0, value), "Absorption", null, value > 0.0);
        }));
        register(simple("minecraft:armor", "Armor", minecraft -> {
            LocalPlayer player = minecraft.player;
            return player == null ? inactive("Armor") : active(player.getArmorValue(), 0.0, 20.0, "Armor");
        }));
        register(simple("minecraft:hunger", "Hunger", minecraft -> {
            LocalPlayer player = minecraft.player;
            return player == null ? inactive("Hunger")
                    : active(player.getFoodData().getFoodLevel(), 0.0, 20.0, "Hunger");
        }));
        register(simple("minecraft:air", "Air", minecraft -> {
            LocalPlayer player = minecraft.player;
            if (player == null) return inactive("Air");
            int maximum = player.getMaxAirSupply();
            int current = Math.min(maximum, player.getAirSupply());
            return new NumericBarSnapshot(current, 0.0, maximum, "Air", null,
                    player.isUnderWater() || current < maximum);
        }));
        register(simple("minecraft:experience", "Experience", minecraft -> {
            LocalPlayer player = minecraft.player;
            return player == null ? inactive("Experience")
                    : active(player.experienceProgress, 0.0, 1.0, "Experience");
        }));
        register(simple("minecraft:mount_health", "Mount Health", minecraft -> {
            LocalPlayer player = minecraft.player;
            if (player != null && player.getVehicle() instanceof LivingEntity living) {
                return active(living.getHealth(), 0.0, living.getMaxHealth(), "Mount Health");
            }
            return inactive("Mount Health");
        }));
        register(simple("minecraft:mount_jump", "Mount Jump", minecraft -> {
            LocalPlayer player = minecraft.player;
            boolean active = player != null && player.getVehicle() instanceof PlayerRideableJumping;
            return new NumericBarSnapshot(player == null ? 0.0 : player.getJumpRidingScale(),
                    0.0, 1.0, "Mount Jump", null, active);
        }));
    }

    private BarSourceRegistry() {
    }

    public static synchronized void register(NumericBarSource source) {
        Objects.requireNonNull(source, "source");
        if (SOURCES.putIfAbsent(source.id(), source) != null) {
            throw new IllegalArgumentException("Duplicate numeric HUD source " + source.id());
        }
    }

    public static synchronized void initializeOptionalIntegrations() {
        if (optionalIntegrationsInitialized) return;
        optionalIntegrationsInitialized = true;
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsSpellbooksManaSource.create().ifPresent(BarSourceRegistry::register);
        }
    }

    public static synchronized NumericBarSource get(String id) {
        return SOURCES.get(id);
    }

    public static synchronized List<NumericBarSource> all() {
        return List.copyOf(SOURCES.values());
    }

    public static synchronized String nextId(String current) {
        List<String> ids = new ArrayList<>(SOURCES.keySet());
        if (ids.isEmpty()) return "";
        int index = ids.indexOf(current);
        return ids.get((index + 1 + ids.size()) % ids.size());
    }

    private static NumericBarSource simple(String id, String name,
                                           Function<Minecraft, NumericBarSnapshot> sampler) {
        return new NumericBarSource() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String displayName() {
                return name;
            }

            @Override
            public NumericBarSnapshot snapshot(Minecraft minecraft) {
                return sampler.apply(minecraft);
            }
        };
    }

    private static NumericBarSnapshot active(double current, double minimum, double maximum, String name) {
        return new NumericBarSnapshot(current, minimum, Math.max(minimum + 0.0001, maximum), name, null, true);
    }

    private static NumericBarSnapshot inactive(String name) {
        return new NumericBarSnapshot(0.0, 0.0, 1.0, name, null, false);
    }
}
