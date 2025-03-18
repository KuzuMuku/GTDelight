package kuzuMuku.gtDelight.item;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static kuzuMuku.gtDelight.GTDelight.CREATIVE_MODE_TABS;
import static kuzuMuku.gtDelight.GTDelight.MODID;

public class GTDelightItems {
    public static final List<Item> ItemList = new ArrayList<>();


    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static void initForge(IEventBus context){






        register("soup", 2, 0.5F);
        register("soup1", 2, 0.5F, true);
        register("soup2", 2, 0.5F, true, new MobEffectInstance(MobEffects.BLINDNESS, 30, 2,false ,true), 30);







        //Done register
        ITEMS.register(context);
        CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
                .withTabsBefore(CreativeModeTabs.COMBAT)
                .icon(() -> ItemList.get(0).getDefaultInstance())
                .displayItems((parameters, output) -> ItemList.forEach(output::accept)).build());
    }
    public static void register(String name,int nutrition, float sat) {
        register(name, nutrition, sat, false, null, 0);
    }

    public static void register(String name,int nutrition, float sat, boolean fastEat) {
        register(name, nutrition, sat, fastEat, null, 0);
    }
    public static void register(String name, int nutrition, float sat, boolean fastEat, @Nullable MobEffectInstance effect, float effectChance) {
        FoodProperties.Builder builder = new FoodProperties.Builder();
        if(fastEat) builder.fast();
        builder.nutrition(nutrition);
        builder.saturationMod(sat);
        if(effect != null)builder.effect(() -> effect, effectChance);

        ITEMS.register(name, () -> addToList(new Item(new Item.Properties().food(builder.alwaysEat().build()))));
    }
    private static Item addToList(Item item){
        ItemList.add(item);
        return item;
    }

}
