package kuzuMuku.gtDelight.common.machine.simple;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.gui.editor.EditableUI;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputItem;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;


import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.BiFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HarvesterMachine extends TieredEnergyMachine implements IAutoOutputItem, IFancyUIMachine, IMachineLife, IWorkable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(HarvesterMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    // Auto Output 相关字段
    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingItems;
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean autoOutputItems;
    @Persisted
    protected final NotifiableItemStackHandler outputInventory;
    @Persisted
    protected boolean allowInputFromOutputSideItems;

    // 能量和状态
    @Nullable
    protected TickableSubscription autoOutputSubs, workingSubs;
    @Nullable
    protected ISubscription outputInventorySubs, energySubs;
    private static final long ENERGY_COST_PER_OPERATION = 8L;
    private final int harvestRange;

    // 工作进度
    @Persisted
    private int progress = 0;
    private final int maxProgress = 100;
    @Persisted
    @DescSynced
    private boolean isWorkingEnabled = true;
    @Persisted
    @DescSynced
    @RequireRerender
    private boolean active = false;

    public HarvesterMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier);
        this.harvestRange = (int) Math.pow(3,tier);
        this.outputInventory = createOutputItemHandler();
        setOutputFacingItems(getFrontFacing());
    }

    //////////////////////////////////////
    // ****** Initialization *******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected NotifiableItemStackHandler createOutputItemHandler() {
        return new NotifiableItemStackHandler(this, 9, IO.BOTH, IO.OUT); // 3x3输出槽
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (isRemote()) return;

        outputInventorySubs = outputInventory.addChangedListener(this::updateAutoOutputSubscription);
        energySubs = energyContainer.addChangedListener(this::updateWorkingSubscription);

        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
        }
        updateWorkingSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (outputInventorySubs != null) outputInventorySubs.unsubscribe();
        if (energySubs != null) energySubs.unsubscribe();
    }

    //////////////////////////////////////
    // ********* 核心逻辑 **********//
    //////////////////////////////////////
    private void updateWorkingSubscription() {
        if (canWork()) {
            workingSubs = subscribeServerTick(workingSubs, this::harvestTick);
            active = true;
        } else {
            if (workingSubs != null) {
                workingSubs.unsubscribe();
                workingSubs = null;
            }
            active = false;
            progress = 0;
        }
    }

    private void harvestTick() {
        if (!canWork()) {
            updateWorkingSubscription();
            return;
        }

        if (++progress >= maxProgress) {
            harvestCrops();
            progress = 0;
            energyContainer.removeEnergy(ENERGY_COST_PER_OPERATION);
        }
    }

    private boolean canWork() {
        return isWorkingEnabled
                && energyContainer.getEnergyStored() >= ENERGY_COST_PER_OPERATION
               // && !getLevel().isClientSide
                && checkCropsAvailable();
    }

    private boolean checkCropsAvailable() {
        BlockPos center = getPos(); // 假设机器下方是耕地
        int radius = harvestRange;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.offset(x, 0, z);
                if (isMatureCrop(pos)) return true;
            }
        }
        return false;
    }

    private void harvestCrops() {
        BlockPos center = getPos().below();
        int radius = harvestRange;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.offset(x, 0, z);
                harvestBlock(pos);
            }
        }
    }

    private boolean isMatureCrop(BlockPos pos) {
        BlockState state = getLevel().getBlockState(pos);
        // 示例：检测小麦成熟（需要根据实际作物扩展）
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return state.getValue(CropBlock.AGE) == cropBlock.getMaxAge();
        }
        return false;
    }

    private void harvestBlock(BlockPos pos) {
        BlockState state = getLevel().getBlockState(pos);
        if (!isMatureCrop(pos)) return;

        // 模拟玩家收获
        LootParams.Builder params = new LootParams.Builder((ServerLevel) getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state);

        state.getDrops(params).forEach(this::tryInsertToOutput);
        getLevel().destroyBlock(pos, false);
    }

    private void tryInsertToOutput(ItemStack stack) {
        for (int i = 0; i < outputInventory.getSlots(); i++) {
            stack = outputInventory.insertItem(i, stack, false);
            if (stack.isEmpty()) break;
        }
    }

    @Override
    public boolean isAutoOutputItems() {
        return false;
    }

    //////////////////////////////////////
    // ******* 自动输出 *******//
    //////////////////////////////////////
    @Override
    public void setAutoOutputItems(boolean allow) {
        this.autoOutputItems = allow;
        updateAutoOutputSubscription();
    }

    @Override
    public boolean isAllowInputFromOutputSideItems() {
        return this.allowInputFromOutputSideItems;
    }

    @Override
    public void setAllowInputFromOutputSideItems(boolean allowInputFromOutputSideItems) {
        this.allowInputFromOutputSideItems = allowInputFromOutputSideItems;
    }

    @Override
    public Direction getOutputFacingItems() {
        return this.outputFacingItems;
    }

    @Override
    public void setOutputFacingItems(@Nullable Direction facing) {
        this.outputFacingItems = facing;
        updateAutoOutputSubscription();
    }

    protected void updateAutoOutputSubscription() {
        if (autoOutputItems && outputFacingItems != null &&
                !outputInventory.isEmpty() &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), outputFacingItems)) {
            autoOutputSubs = subscribeServerTick(autoOutputSubs, this::checkAutoOutput);
        } else if (autoOutputSubs != null) {
            autoOutputSubs.unsubscribe();
            autoOutputSubs = null;
        }
    }

    protected void checkAutoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            outputInventory.exportToNearby(outputFacingItems);
            updateAutoOutputSubscription();
        }
    }

//////////////////////////////////////
// ********** GUI ***********//
//////////////////////////////////////

    public static BiFunction<ResourceLocation, Integer, EditableMachineUI> EDITABLE_UI_CREATOR = Util
            .memoize((path, inventorySize) -> new EditableMachineUI("misc", path, () -> {

                var template = createTemplate(inventorySize).createDefault();
                var energyBar = createEnergyBar().createDefault();
                var batterySlot = createBatterySlot().createDefault();

                // 能量条和电池槽组合
                var energyGroup = new WidgetGroup(0, 0, energyBar.getSize().width, energyBar.getSize().height + 20);
                batterySlot.setSelfPosition(
                        new Position((energyBar.getSize().width - 18) / 2, energyBar.getSize().height + 1));
                energyGroup.addWidget(energyBar);
                energyGroup.addWidget(batterySlot);

                // 主界面组合
                var group = new WidgetGroup(0, 0,
                        Math.max(energyGroup.getSize().width + template.getSize().width + 4 + 8, 172),
                        Math.max(template.getSize().height + 8, energyGroup.getSize().height + 8));
                var size = group.getSize();
                energyGroup.setSelfPosition(new Position(3, (size.height - energyGroup.getSize().height) / 2));

                template.setSelfPosition(new Position(
                        (size.width - energyGroup.getSize().width - 4 - template.getSize().width) / 2 + 2 +
                                energyGroup.getSize().width + 2,
                        (size.height - template.getSize().height) / 2));

                group.addWidget(energyGroup);
                group.addWidget(template);
                return group;
            }, (template, machine) -> {
                if (machine instanceof HarvesterMachine harvesterMachine) {
                    // 绑定模板、能量条和电池槽
                    createTemplate(inventorySize).setupUI(template, harvesterMachine);
                    createEnergyBar().setupUI(template, harvesterMachine);
                    createBatterySlot().setupUI(template, harvesterMachine);
                }
            }));

    /**
     * 创建电池槽
     */
    protected static EditableUI<SlotWidget, HarvesterMachine> createBatterySlot() {
        return new EditableUI<>("battery_slot", SlotWidget.class, () -> {
            var slotWidget = new SlotWidget();
            slotWidget.setBackground(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY);
            return slotWidget;
        }, (slotWidget, machine) -> {
            slotWidget.setHandlerSlot(machine.outputInventory, 0);
            slotWidget.setCanPutItems(true);
            slotWidget.setCanTakeItems(true);
            slotWidget.setHoverTooltips(LangHandler.getMultiLang("gtceu.gui.charger_slot.tooltip",
                    GTValues.VNF[machine.getTier()], GTValues.VNF[machine.getTier()]).toArray(new MutableComponent[0]));
        });
    }


    /**
     * 创建模板（3x3 输出槽位）
     */
    protected static EditableUI<WidgetGroup, HarvesterMachine> createTemplate(int inventorySize) {
        return new EditableUI<>("functional_container", WidgetGroup.class, () -> {
            int rowSize = (int) Math.sqrt(inventorySize);
            WidgetGroup main = new WidgetGroup(0, 0, rowSize * 18 + 8 + 20, rowSize * 18 + 8);

            for (int y = 0; y < rowSize; y++) {
                for (int x = 0; x < rowSize; x++) {
                    int index = y * rowSize + x;
                    SlotWidget slotWidget = new SlotWidget();
                    slotWidget.initTemplate();
                    slotWidget.setSelfPosition(new Position(24 + x * 18, 4 + y * 18));
                    slotWidget.setBackground(GuiTextures.SLOT);
                    slotWidget.setId("slot_" + index);
                    main.addWidget(slotWidget);
                }
            }

            SlotWidget baitSlotWidget = new SlotWidget();
            baitSlotWidget.initTemplate();
            baitSlotWidget
                    .setSelfPosition(new Position(4, (main.getSize().height - baitSlotWidget.getSize().height) / 2));
            baitSlotWidget.setBackground(GuiTextures.SLOT, GuiTextures.STRING_SLOT_OVERLAY);
            baitSlotWidget.setId("bait_slot");
            main.addWidget(baitSlotWidget);

            main.setBackground(GuiTextures.BACKGROUND_INVERSE);
            return main;
        }, (group, machine) -> {
            // 绑定输出槽位
            WidgetUtils.widgetByIdForEach(group, "^slot_[0-9]+$", SlotWidget.class, slot -> {
                var index = WidgetUtils.widgetIdIndex(slot);
                if (index >= 0 && index < machine.outputInventory.getSlots()) {
                    slot.setHandlerSlot(machine.outputInventory, index);
                    slot.setCanTakeItems(true);
                    slot.setCanPutItems(false);
                }
            });
        });
    }

    //////////////////////////////////////
    // ******* Rendering ********//
    //////////////////////////////////////
    @Override
    public ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                    Direction side) {
        if (toolTypes.contains(GTToolType.WRENCH)) {
            if (!player.isShiftKeyDown()) {
                if (!hasFrontFacing() || side != getFrontFacing()) {
                    return GuiTextures.TOOL_IO_FACING_ROTATION;
                }
            }
        } else if (toolTypes.contains(GTToolType.SCREWDRIVER)) {
            if (side == getOutputFacingItems()) {
                return GuiTextures.TOOL_ALLOW_INPUT;
            }
        } else if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            return this.isWorkingEnabled ? GuiTextures.TOOL_PAUSE : GuiTextures.TOOL_START;
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }

    //////////////////////////////////////
    // ******** 工具交互 *********//
    //////////////////////////////////////
    @Override
    protected InteractionResult onWrenchClick(Player player, InteractionHand hand,
                                              Direction face, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            setOutputFacingItems(face);
            return InteractionResult.SUCCESS;
        }
        return super.onWrenchClick(player, hand, face, hit);
    }

    @Override
    public boolean isWorkingEnabled() {
        return isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.isWorkingEnabled = workingEnabled;
        updateWorkingSubscription();
    }


    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}