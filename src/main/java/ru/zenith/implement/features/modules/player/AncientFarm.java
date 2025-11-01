package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.RotationUpdateEvent;
import ru.zenith.implement.features.draggables.Notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AncientFarm extends Module {

    public static AncientFarm getInstance() {
        return Instance.get(AncientFarm.class);
    }

    // Основные настройки
    ValueSetting tunnelLength = new ValueSetting("Tunnel Length", "Maximum tunnel length")
            .setValue(500f).range(100f, 2000f);

    ValueSetting blocksBetweenTNT = new ValueSetting("Blocks Between TNT", "How many blocks to mine before placing TNT")
            .setValue(10f).range(5f, 50f);

    ValueSetting tntMiningLength = new ValueSetting("TNT Mining Length", "Tunnel length mined by each TNT")
            .setValue(5f).range(3f, 15f);

    ValueSetting scanRadius = new ValueSetting("Scan Radius", "Radius to scan for ancient debris after explosion")
            .setValue(15f).range(10f, 20f);

    ValueSetting safeDistance = new ValueSetting("Safe Distance", "Distance to retreat from TNT")
            .setValue(15f).range(10f, 20f);

    ValueSetting operationDelay = new ValueSetting("Operation Delay", "Delay between operations (ms)")
            .setValue(200f).range(100f, 1000f);

    // Группа настроек TNT
    BooleanSetting useTNT = new BooleanSetting("Use TNT", "Use TNT for mining")
            .setValue(true);

    ValueSetting tntPlaceDistance = new ValueSetting("TNT Place Distance", "Distance from player to place TNT")
            .setValue(3f).range(2f, 8f).visible(() -> useTNT.isValue());

    // Группа настроек безопасности
    BooleanSetting autoFireResistance = new BooleanSetting("Auto Fire Resistance", "Automatically drink fire resistance potions")
            .setValue(true);

    // Группа настроек инвентаря
    BooleanSetting dropNetherrack = new BooleanSetting("Drop Netherrack", "Automatically drop netherrack when inventory full")
            .setValue(true);

    // Группа настроек интерфейса
    BooleanSetting showNotifications = new BooleanSetting("Show Notifications", "Show notifications about operations")
            .setValue(true);

    BooleanSetting debugMode = new BooleanSetting("Debug Mode", "Show detailed debug information")
            .setValue(false);

    // Состояние модуля
    @NonFinal
    private FarmState currentState = FarmState.IDLE;

    @NonFinal
    private BlockPos startPos;

    @NonFinal
    private Direction tunnelDirection;

    @NonFinal
    private BlockPos lastTntPos;

    @NonFinal
    private int tunnelProgress = 0;

    @NonFinal
    private int blocksMinedSinceLastTNT = 0;

    @NonFinal
    private int tntCount = 0;

    @NonFinal
    private final List<BlockPos> foundDebris = new ArrayList<>();

    @NonFinal
    private long lastStateChangeTime = 0;

    @NonFinal
    private long lastPotionTime = 0;

    @NonFinal
    private long lastTntCheckTime = 0;

    @NonFinal
    private long lastProcessTime = 0;

    @NonFinal
    private int previousHotbarSlot = 0;

    @NonFinal
    private boolean waitingForBaritone = false;

    @NonFinal
    private long baritoneStartTime = 0;

    @NonFinal
    private long miningStartTime = 0;

    @NonFinal
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AncientFarm() {
        super("AncientFarm", ModuleCategory.PLAYER);

        // Группировка настроек для удобного GUI
        GroupSetting miningGroup = new GroupSetting("Mining Settings", "Configure mining parameters")
                .settings(tunnelLength, blocksBetweenTNT, tntMiningLength)
                .setValue(true);

        GroupSetting tntGroup = new GroupSetting("TNT Settings", "Configure TNT usage")
                .settings(useTNT, tntPlaceDistance, scanRadius, safeDistance)
                .setValue(true);

        GroupSetting safetyGroup = new GroupSetting("Safety Settings", "Configure safety features")
                .settings(autoFireResistance)
                .setValue(true);

        GroupSetting inventoryGroup = new GroupSetting("Inventory Settings", "Configure inventory management")
                .settings(dropNetherrack)
                .setValue(true);

        GroupSetting interfaceGroup = new GroupSetting("Interface Settings", "Configure display options")
                .settings(showNotifications, debugMode, operationDelay)
                .setValue(true);

        setup(miningGroup, tntGroup, safetyGroup, inventoryGroup, interfaceGroup);
    }

    @Override
    public void activate() {
        if (mc.player == null || mc.world == null) return;

        startPos = mc.player.getBlockPos();
        tunnelDirection = getPlayerDirection();
        currentState = FarmState.CHECK_FIRE_RESISTANCE;
        tunnelProgress = 0;
        blocksMinedSinceLastTNT = 0;
        tntCount = 0;
        foundDebris.clear();
        lastStateChangeTime = System.currentTimeMillis();
        previousHotbarSlot = mc.player.getInventory().selectedSlot;
        waitingForBaritone = false;
        miningStartTime = System.currentTimeMillis();

        if (showNotifications.isValue()) {
            Notifications.getInstance().addList(Text.literal("§aAncientFarm started - Direction: " + tunnelDirection +
                    " | Blocks between TNT: " + (int)blocksBetweenTNT.getValue()), 3000);
        }

        debugLog("Module activated - Direction: " + tunnelDirection + ", Blocks between TNT: " + (int)blocksBetweenTNT.getValue());
        super.activate();
    }

    @Override
    public void deactivate() {
        currentState = FarmState.IDLE;
        stopBaritone();
        scheduler.shutdown();

        if (mc.player != null) {
            mc.player.getInventory().selectedSlot = previousHotbarSlot;
        }

        long miningTime = (System.currentTimeMillis() - miningStartTime) / 1000;
        if (showNotifications.isValue()) {
            Notifications.getInstance().addList(Text.literal("§cAncientFarm stopped | Progress: " + tunnelProgress +
                    " blocks | TNT used: " + tntCount + " | Time: " + miningTime + "s"), 3000);
        }

        debugLog("Module deactivated - Progress: " + tunnelProgress + " blocks, TNT used: " + tntCount);
        super.deactivate();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (!isState() || mc.player == null || mc.world == null) return;

        long currentTime = System.currentTimeMillis();
        long delay = (long) operationDelay.getValue();

        if (currentTime - lastProcessTime < delay) {
            return;
        }
        lastProcessTime = currentTime;

        handleFireResistance();

        // Обработка состояний с приоритетом
        switch (currentState) {
            case CHECK_FIRE_RESISTANCE -> handleFireResistanceCheck();
            case MINE_BLOCKS -> handleMineBlocks();
            case CHECK_TNT_READY -> handleCheckTntReady();
            case PLACE_TNT -> handlePlaceTnt();
            case ACTIVATE_TNT -> handleActivateTnt();
            case WAIT_EXPLOSION -> handleWaitExplosion();
            case SCAN_DEBRIS -> handleScanDebris();
            case MINE_DEBRIS -> handleMineDebris();
            case DROP_NETHERACK -> handleDropNetherrack();
        }
    }

    private void handleFireResistance() {
        if (!autoFireResistance.isValue()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPotionTime > 60000 && !hasFireResistanceEffect() && hasFireResistancePotion()) {
            drinkFireResistancePotion();
            lastPotionTime = currentTime;
            debugLog("Drank fire resistance potion");
        }
    }

    private void handleFireResistanceCheck() {
        if (!autoFireResistance.isValue() || hasFireResistanceEffect() ||
                System.currentTimeMillis() - lastStateChangeTime > 5000) {
            setState(FarmState.MINE_BLOCKS);
        }
    }

    private void handleMineBlocks() {
        if (tunnelProgress >= tunnelLength.getValue()) {
            long miningTime = (System.currentTimeMillis() - miningStartTime) / 1000;
            if (showNotifications.isValue()) {
                Notifications.getInstance().addList(Text.literal("§6AncientFarm completed! | Total: " + tunnelProgress +
                        " blocks | TNT used: " + tntCount + " | Time: " + miningTime + "s"), 5000);
            }
            deactivate();
            return;
        }

        if (waitingForBaritone) {
            // Проверяем достигли ли целевой позиции
            BlockPos targetPos = startPos.offset(tunnelDirection, tunnelProgress);
            if (isPlayerNearPosition(targetPos, 2)) {
                waitingForBaritone = false;
                // Увеличиваем прогресс когда достигли позиции
                tunnelProgress++;
                blocksMinedSinceLastTNT++;
                debugLog("Reached position, progress: " + tunnelProgress + ", blocks since TNT: " + blocksMinedSinceLastTNT);

                // Сразу проверяем нужно ли ставить TNT после увеличения прогресса
                if (blocksMinedSinceLastTNT >= blocksBetweenTNT.getValue() && useTNT.isValue()) {
                    setState(FarmState.CHECK_TNT_READY);
                    return;
                }
            }
            // Таймаут 8 секунд
            else if (System.currentTimeMillis() - baritoneStartTime > 8000) {
                debugLog("Baritone timeout - forcing progress");
                waitingForBaritone = false;
                tunnelProgress++;
                blocksMinedSinceLastTNT++;

                if (blocksMinedSinceLastTNT >= blocksBetweenTNT.getValue() && useTNT.isValue()) {
                    setState(FarmState.CHECK_TNT_READY);
                    return;
                }
            }
            return;
        }

        // Запускаем Baritone до следующей позиции
        BlockPos targetPos = startPos.offset(tunnelDirection, tunnelProgress);
        startBaritoneGoTo(Vec3d.ofCenter(targetPos));
        waitingForBaritone = true;
        baritoneStartTime = System.currentTimeMillis();

        debugLog("Moving to next position: " + targetPos + " | Progress: " + tunnelProgress + "/" + (int)tunnelLength.getValue());
    }

    private void handleCheckTntReady() {
        if (!useTNT.isValue() || !hasTntInInventory()) {
            // Если TNT отключен или нет TNT, продолжаем копать
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
            return;
        }

        // Находим позицию для TNT
        BlockPos currentPos = startPos.offset(tunnelDirection, tunnelProgress);
        lastTntPos = findOptimalTntPosition(currentPos);

        if (lastTntPos != null) {
            setState(FarmState.PLACE_TNT);
            showNotification("§ePlacing TNT | Progress: " + tunnelProgress + " blocks");
        } else {
            showNotification("§cNo safe position for TNT - continuing mining");
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
        }
    }

    private void handlePlaceTnt() {
        if (lastTntPos == null || !hasTntInInventory()) {
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
            return;
        }

        if (!isPlayerNearPosition(lastTntPos, (int)tntPlaceDistance.getValue())) {
            startBaritoneGoTo(Vec3d.ofCenter(lastTntPos));
            waitingForBaritone = true;
            baritoneStartTime = System.currentTimeMillis();
            return;
        }

        if (placeTntBlock(lastTntPos)) {
            scheduler.schedule(this::delayedActivation, 500, TimeUnit.MILLISECONDS);
        } else {
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
        }
    }

    private void delayedActivation() {
        if (isState() && currentState == FarmState.PLACE_TNT) {
            setState(FarmState.ACTIVATE_TNT);
        }
    }

    private void handleActivateTnt() {
        if (lastTntPos == null) {
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
            return;
        }

        if (activateTnt(lastTntPos)) {
            tntCount++;
            retreatToSafeDistance(lastTntPos);
            setState(FarmState.WAIT_EXPLOSION);
            lastTntCheckTime = System.currentTimeMillis();

            if (showNotifications.isValue()) {
                Notifications.getInstance().addList(Text.literal("§aTNT #" + tntCount + " activated! | Mining " +
                        (int)tntMiningLength.getValue() + " blocks with explosion"), 2000);
            }
        } else {
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
        }
    }

    private void handleWaitExplosion() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTntCheckTime > 15000) {
            showNotification("§cTNT timeout - continuing mining");
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
        } else if (isTntExploded(lastTntPos)) {
            showNotification("§aTNT exploded! Scanning for debris...");
            setState(FarmState.SCAN_DEBRIS);
        }
    }

    private void handleScanDebris() {
        if (System.currentTimeMillis() - lastStateChangeTime > 2000) {
            foundDebris.clear();
            foundDebris.addAll(scanForAncientDebris(lastTntPos));

            if (!foundDebris.isEmpty()) {
                showNotification("§aFound " + foundDebris.size() + " ancient debris! Mining...");
                setState(FarmState.MINE_DEBRIS);
            } else {
                // После взрыва TNT увеличиваем прогресс туннеля
                tunnelProgress += (int)tntMiningLength.getValue();
                blocksMinedSinceLastTNT = 0;
                showNotification("§bNo debris found | Tunnel progress: " + tunnelProgress + " blocks");
                setState(FarmState.MINE_BLOCKS);
            }
        }
    }

    private void handleMineDebris() {
        if (foundDebris.isEmpty()) {
            // После добычи обломков увеличиваем прогресс туннеля
            tunnelProgress += (int)tntMiningLength.getValue();
            blocksMinedSinceLastTNT = 0;
            setState(FarmState.MINE_BLOCKS);
            return;
        }

        BlockPos debrisPos = foundDebris.get(0);

        if (!waitingForBaritone) {
            startBaritoneGoTo(Vec3d.ofCenter(debrisPos));
            waitingForBaritone = true;
            baritoneStartTime = System.currentTimeMillis();
            return;
        }

        if (isBlockBroken(debrisPos)) {
            foundDebris.remove(0);
            debugLog("Mined debris at " + debrisPos);
            waitingForBaritone = false;
        }

        // Таймаут для добычи обломков
        if (System.currentTimeMillis() - baritoneStartTime > 10000) {
            debugLog("Debris mining timeout");
            waitingForBaritone = false;
            foundDebris.remove(0);
        }

        if (isInventoryFull()) {
            setState(FarmState.DROP_NETHERACK);
        }
    }

    private void handleDropNetherrack() {
        if (dropNetherrack.isValue()) {
            dropNetherrackFromInventory();
        }
        setState(FarmState.MINE_BLOCKS);
    }

    // Вспомогательные методы
    private void setState(FarmState newState) {
        if (currentState != newState) {
            debugLog("State change: " + currentState + " -> " + newState);
            currentState = newState;
            lastStateChangeTime = System.currentTimeMillis();
        }
    }

    private void showNotification(String message) {
        if (showNotifications.isValue()) {
            Notifications.getInstance().addList(Text.literal(message), 2000);
        }
    }

    private void debugLog(String message) {
        if (debugMode.isValue()) {
            System.out.println("[AncientFarm] " + message);
        }
    }

    private Direction getPlayerDirection() {
        float yaw = mc.player.getYaw();
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) return Direction.SOUTH;
        else if (yaw >= 45 && yaw < 135) return Direction.WEST;
        else if (yaw >= 135 && yaw < 225) return Direction.NORTH;
        else return Direction.EAST;
    }

    private BlockPos findOptimalTntPosition(BlockPos basePos) {
        // Приоритет: вперед, сверху, по бокам
        BlockPos[] positions = {
                basePos.offset(tunnelDirection, (int)tntPlaceDistance.getValue()),
                basePos.offset(Direction.UP),
                basePos.offset(tunnelDirection.rotateYClockwise()),
                basePos.offset(tunnelDirection.rotateYCounterclockwise())
        };

        for (BlockPos pos : positions) {
            if (isValidTntPosition(pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isValidTntPosition(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }

        BlockPos below = pos.offset(Direction.DOWN);
        if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return false;
        }

        return isPositionSafeForPlayer(pos);
    }

    private boolean isPositionSafeForPlayer(BlockPos tntPos) {
        if (mc.player == null) return false;
        Vec3d playerPos = mc.player.getPos();
        Vec3d tntVec = Vec3d.ofCenter(tntPos);
        return playerPos.distanceTo(tntVec) > 4;
    }

    private void retreatToSafeDistance(BlockPos tntPos) {
        Direction retreatDir = tunnelDirection.getOpposite();
        BlockPos retreatPos = findSafeRetreatPosition(tntPos, retreatDir);

        if (retreatPos != null) {
            startBaritoneGoTo(Vec3d.ofCenter(retreatPos));
            waitingForBaritone = true;
            baritoneStartTime = System.currentTimeMillis();
        }
    }

    private BlockPos findSafeRetreatPosition(BlockPos tntPos, Direction direction) {
        for (int i = (int)safeDistance.getValue(); i >= 5; i--) {
            BlockPos testPos = tntPos.offset(direction, i);
            if (isPositionSafeForRetreat(testPos)) {
                return testPos;
            }
        }
        return null;
    }

    private boolean isPositionSafeForRetreat(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }

        BlockPos below = pos.offset(Direction.DOWN);
        return mc.world.getBlockState(below).isSolidBlock(mc.world, below);
    }

    private boolean isPlayerNearPosition(BlockPos pos, double maxDistance) {
        if (mc.player == null) return false;
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = Vec3d.ofCenter(pos);
        double distance = playerPos.distanceTo(targetPos);
        return distance <= maxDistance;
    }

    private List<BlockPos> scanForAncientDebris(BlockPos center) {
        List<BlockPos> debrisList = new ArrayList<>();
        int radius = (int)scanRadius.getValue();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos scanPos = center.add(x, y, z);
                    if (mc.world.isChunkLoaded(scanPos)) {
                        BlockState state = mc.world.getBlockState(scanPos);
                        if (state.getBlock() == Blocks.ANCIENT_DEBRIS) {
                            debrisList.add(scanPos);
                        }
                    }
                }
            }
        }
        return debrisList;
    }

    private boolean placeTntBlock(BlockPos pos) {
        int tntSlot = findItemInInventory(Items.TNT);
        if (tntSlot == -1) {
            showNotification("§cNo TNT in inventory");
            return false;
        }

        previousHotbarSlot = mc.player.getInventory().selectedSlot;
        int hotbarSlot = tntSlot % 9;
        mc.player.getInventory().selectedSlot = hotbarSlot;

        lookAtPosition(Vec3d.ofCenter(pos));

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));

        boolean placed = result.isAccepted();

        if (placed) {
            debugLog("TNT placed at " + pos);
        } else {
            showNotification("§cFailed to place TNT");
            mc.player.getInventory().selectedSlot = previousHotbarSlot;
        }

        return placed;
    }

    private boolean activateTnt(BlockPos pos) {
        int flintSlot = findItemInInventory(Items.FLINT_AND_STEEL);
        if (flintSlot == -1) {
            showNotification("§cNo flint and steel!");
            return false;
        }

        int hotbarSlot = flintSlot % 9;
        mc.player.getInventory().selectedSlot = hotbarSlot;

        lookAtPosition(Vec3d.ofCenter(pos));

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));

        boolean activated = result.isAccepted();

        mc.player.getInventory().selectedSlot = previousHotbarSlot;

        if (!activated) {
            showNotification("§cFailed to activate TNT");
        }

        return activated;
    }

    private void lookAtPosition(Vec3d pos) {
        if (mc.player == null) return;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = pos.subtract(eyePos).normalize();

        float yaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float pitch = (float)Math.toDegrees(Math.asin(-direction.y));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    // Baritone методы
    private void startBaritoneGoTo(Vec3d pos) {
        executeBaritoneCommand("#goto " + (int)pos.x + " " + (int)pos.y + " " + (int)pos.z);
        debugLog("Started Baritone goto: " + pos);
    }

    private void stopBaritone() {
        executeBaritoneCommand("#stop");
        debugLog("Stopped Baritone");
    }

    private void executeBaritoneCommand(String command) {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatMessage(command);
        }
    }

    // Инвентарь и предметы
    private boolean hasTntInInventory() {
        return findItemInInventory(Items.TNT) != -1;
    }

    private boolean hasFireResistancePotion() {
        return findItemInInventory(Items.POTION) != -1;
    }

    private void drinkFireResistancePotion() {
        int potionSlot = findItemInInventory(Items.POTION);
        if (potionSlot != -1) {
            mc.interactionManager.clickSlot(0, potionSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }

    private boolean hasFireResistanceEffect() {
        return mc.player != null && mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE);
    }

    private int findItemInInventory(net.minecraft.item.Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInventoryFull() {
        return mc.player.getInventory().getEmptySlot() == -1;
    }

    private void dropNetherrackFromInventory() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.NETHERRACK) {
                mc.interactionManager.clickSlot(0, i, 1, net.minecraft.screen.slot.SlotActionType.THROW, mc.player);
                break;
            }
        }
    }

    private boolean isTntExploded(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir();
    }

    private boolean isBlockBroken(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir();
    }

    private enum FarmState {
        IDLE,
        CHECK_FIRE_RESISTANCE,
        MINE_BLOCKS,
        CHECK_TNT_READY,
        PLACE_TNT,
        ACTIVATE_TNT,
        WAIT_EXPLOSION,
        SCAN_DEBRIS,
        MINE_DEBRIS,
        DROP_NETHERACK
    }
}