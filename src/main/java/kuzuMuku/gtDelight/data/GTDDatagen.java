package kuzuMuku.gtDelight.data;

import com.tterrag.registrate.providers.ProviderType;
import kuzuMuku.gtDelight.data.lang.ChineseLangHandler;
import kuzuMuku.gtDelight.data.lang.RegistrateCNLangProvider;
import kuzuMuku.gtDelight.registry.GTDRegistration;
import kuzuMuku.gtDelight.data.lang.EnglishLangHandler;

public class GTDDatagen {

    public static final ProviderType<RegistrateCNLangProvider> CNLANG = ProviderType.register("gt_delight_lang", (p, e) -> new RegistrateCNLangProvider(p, e.getGenerator().getPackOutput()));

    public static void init() {
        GTDRegistration.REGISTRATE.addDataGenerator(ProviderType.LANG, EnglishLangHandler::init);
        GTDRegistration.REGISTRATE.addDataGenerator(CNLANG, ChineseLangHandler::init);
    }
}
