package com.cappleapple.myhudnotyours.compat.irons_spellbooks;

import com.cappleapple.myhudnotyours.MyHudNotYours;
import com.cappleapple.myhudnotyours.bar.NumericBarSnapshot;
import com.cappleapple.myhudnotyours.bar.NumericBarSource;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

/** Optional semantic adapter backed by Iron's own synchronized client mana data. */
public final class IronsSpellbooksManaSource implements NumericBarSource {
    public static final String SOURCE_ID = "irons_spellbooks:mana";
    private static final ResourceLocation MAX_MANA_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "max_mana");

    private final Method getPlayerMana;
    private final Method shouldShowManaBar;
    private Holder.Reference<Attribute> maximumMana;
    private boolean invocationFailed;

    private IronsSpellbooksManaSource(Method getPlayerMana, Method shouldShowManaBar) {
        this.getPlayerMana = getPlayerMana;
        this.shouldShowManaBar = shouldShowManaBar;
    }

    public static Optional<NumericBarSource> create() {
        try {
            ClassLoader loader = IronsSpellbooksManaSource.class.getClassLoader();
            Class<?> clientMagicData = Class.forName(
                    "io.redspace.ironsspellbooks.player.ClientMagicData", false, loader);
            Class<?> manaBarOverlay = Class.forName(
                    "io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay", false, loader);
            Method getPlayerMana = clientMagicData.getMethod("getPlayerMana");
            Method shouldShowManaBar = manaBarOverlay.getMethod("shouldShowManaBar", Player.class);
            return Optional.of(new IronsSpellbooksManaSource(getPlayerMana, shouldShowManaBar));
        } catch (ReflectiveOperationException exception) {
            MyHudNotYours.LOGGER.warn(
                    "Iron's Spells 'n Spellbooks is loaded, but its client mana API could not be linked", exception);
            return Optional.empty();
        }
    }

    @Override
    public String id() {
        return SOURCE_ID;
    }

    @Override
    public String displayName() {
        return "Mana";
    }

    @Override
    public NumericBarSnapshot snapshot(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || invocationFailed) return inactive();
        Holder.Reference<Attribute> attribute = maximumManaAttribute();
        if (attribute == null) return inactive();
        try {
            double current = ((Number) getPlayerMana.invoke(null)).doubleValue();
            double maximum = Math.max(1.0, player.getAttributeValue(attribute));
            boolean active = (Boolean) shouldShowManaBar.invoke(null, player);
            return new NumericBarSnapshot(current, 0.0, maximum, "Mana", null, active);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            invocationFailed = true;
            MyHudNotYours.LOGGER.warn("Could not read Iron's synchronized client mana state", exception);
            return inactive();
        }
    }

    private Holder.Reference<Attribute> maximumManaAttribute() {
        if (maximumMana == null) {
            maximumMana = BuiltInRegistries.ATTRIBUTE.getHolder(MAX_MANA_ATTRIBUTE).orElse(null);
            if (maximumMana == null && !invocationFailed) {
                invocationFailed = true;
                MyHudNotYours.LOGGER.warn("Iron's max_mana attribute is unavailable; disabling its HUD source");
            }
        }
        return maximumMana;
    }

    private static NumericBarSnapshot inactive() {
        return new NumericBarSnapshot(0.0, 0.0, 1.0, "Mana", null, false);
    }
}
