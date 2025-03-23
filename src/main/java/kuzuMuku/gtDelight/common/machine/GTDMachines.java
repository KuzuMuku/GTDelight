package kuzuMuku.gtDelight.common.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import kuzuMuku.gtDelight.GTDelight;
import kuzuMuku.gtDelight.common.GTDCreativeModeTabs;
import kuzuMuku.gtDelight.common.machine.simple.HarvesterMachine;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.registerTieredMachines;
import static kuzuMuku.gtDelight.registry.GTDRegistration.REGISTRATE;

public class GTDMachines {
    static {
        REGISTRATE.creativeModeTab(() -> GTDCreativeModeTabs.MACHINE);
    }

    public static final MachineDefinition[] HARVESTER = registerTieredMachines("harvester", HarvesterMachine::new,
            (tier, builder) -> builder
                    .rotationState(RotationState.ALL)
                    .editableUI(HarvesterMachine.EDITABLE_UI_CREATOR.apply(GTDelight.id("harvester"), (tier + 1) * (tier + 1)))
                    .tieredHullRenderer(GTCEu.id("block/machine/fisher_machine"))
                    .langValue("%s Harvester %s".formatted(VLVH[tier], VLVT[tier]))
                    .tooltips()
                    .register(),
            LV, MV, HV);

    public static void init() {

    }
}
