package kuzuMuku.gtDelight.data.lang;

import com.gregtechceu.gtceu.api.GTValues;
import kuzuMuku.gtDelight.common.machine.GTDMachines;

public class ChineseLangHandler {
    public static void init(RegistrateCNLangProvider provider) {

        for (int tier = GTValues.LV; tier <= GTValues.HV; tier++){
            provider.add(GTDMachines.HARVESTER[tier].getBlock(),GTValues.VNF[tier] + "§r作物收割机");
        }
    }
}