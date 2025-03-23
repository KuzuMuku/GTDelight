package kuzuMuku.gtDelight.common;

import kuzuMuku.gtDelight.data.GTDDatagen;
import kuzuMuku.gtDelight.registry.GTDRegistration;

public class CommonProxy {
    public CommonProxy() {
        init();
    }

    public static void init() {
        GTDRegistration.REGISTRATE.registerRegistrate();

        GTDCreativeModeTabs.init();
        GTDDatagen.init();
    }
}
