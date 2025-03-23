package kuzuMuku.gtDelight.event;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import kuzuMuku.gtDelight.common.machine.GTDMachines;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static kuzuMuku.gtDelight.GTDelight.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandler {
    public static void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        GTDMachines.init();
    }

    public static void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {

    }

    public static void registerDimensionMarkers(GTCEuAPI.RegisterEvent<ResourceLocation, DimensionMarker> event) {

    }

    @SubscribeEvent
    public static void registerMaterials(MaterialEvent event) {

    }
}