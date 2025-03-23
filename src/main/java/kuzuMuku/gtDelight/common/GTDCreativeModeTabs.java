package kuzuMuku.gtDelight.common;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.tterrag.registrate.util.entry.RegistryEntry;
import kuzuMuku.gtDelight.GTDelight;
import net.minecraft.world.item.CreativeModeTab;

import static kuzuMuku.gtDelight.registry.GTDRegistration.REGISTRATE;

public class GTDCreativeModeTabs {

    public static RegistryEntry<CreativeModeTab> MACHINE = REGISTRATE.defaultCreativeTab("machine",
                    builder -> builder.displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("machine", REGISTRATE))
                            .icon(() -> GTMachines.ELECTROLYZER[GTValues.LV].asStack())
                            .title(REGISTRATE.addLang("itemGroup", GTDelight.id("machine"), "GTD Machines"))
                            .build())
            .register();

    public static RegistryEntry<CreativeModeTab> ITEM = REGISTRATE.defaultCreativeTab("item",
                    builder -> builder.displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("item", REGISTRATE))
                            .icon(() -> GTItems.FLUID_CELL.asStack())
                            .title(REGISTRATE.addLang("itemGroup", GTDelight.id("item"), "GTD Items"))
                            .build())
            .register();
    public static RegistryEntry<CreativeModeTab> BLOCK = REGISTRATE.defaultCreativeTab("block",
                    builder -> builder.displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("block", REGISTRATE))
                            .icon(GTBlocks.COIL_CUPRONICKEL::asStack)
                            .title(REGISTRATE.addLang("itemGroup", GTDelight.id("block"), "GTD Blocks"))
                            .build())
            .register();
    public static void init() {

    }
}
