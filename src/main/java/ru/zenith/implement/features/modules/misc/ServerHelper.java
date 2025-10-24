package ru.zenith.implement.features.modules.misc;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.types.EventType;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BindSetting;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.repository.friend.FriendUtils;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerInventoryComponent;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.entity.SimulatedPlayer;
import ru.zenith.common.util.world.ServerUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.math.ProjectionUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.container.SetScreenEvent;
import ru.zenith.implement.events.keyboard.KeyEvent;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.RotationUpdateEvent;
import ru.zenith.implement.events.render.DrawEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.implement.features.draggables.CoolDowns;
import ru.zenith.implement.features.modules.combat.killaura.rotation.*;
import ru.zenith.implement.features.modules.combat.killaura.rotation.angle.LinearSmoothMode;
import ru.zenith.implement.features.modules.render.ProjectilePrediction;

import java.util.*;
import java.util.List;
import java.util.stream.StreamSupport;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerHelper extends Module {
    Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
    List<ServerEvent> serverEvents = new ArrayList<>();
    List<Structure> structures = new ArrayList<>();
    List<KeyBind> keyBindings = new ArrayList<>();
    PointFinder pointFinder = new PointFinder();
    StopWatch itemsWatch = new StopWatch(), shulkerWatch = new StopWatch(), repairWatch = new StopWatch();
    Script script = new Script(), script2 = new Script();
    @NonFinal
    UUID entityUUID;
    Map<Integer, Item> stacks = new HashMap<>();

    BooleanSetting autoLootSetting = new BooleanSetting("Auto Loot", "Steals loot from bots at the event")
            .setValue(true).visible(ServerUtil::isHolyWorld);
    BooleanSetting autoShulkerSetting = new BooleanSetting("Auto Shulker", "Automatically puts loot into shulker")
            .setValue(true).visible(ServerUtil::isHolyWorld);
    BooleanSetting autoRepairSetting = new BooleanSetting("Auto Repair", "Auto repairs armor with experience bubble at low durability")
            .setValue(true).visible(ServerUtil::isHolyWorld);
    BooleanSetting consumablesSetting = new BooleanSetting("Consumables Timer", "Displays the time until the consumable runs out")
            .setValue(true).visible(ServerUtil::isCopyTime);
    BooleanSetting autoPointSetting = new BooleanSetting("Auto Point", "Displays information on the event")
            .setValue(true).visible(ServerUtil::isFunTime);

    @Compile
    @Initialization
    @VMProtect(type = VMProtectType.VIRTUALIZATION)
    public void initialize() {
        keyBindings.add(new KeyBind(Items.FIREWORK_STAR, new BindSetting("Anti Fly", "Anti Fly Key").visible(ServerUtil::isReallyWorld), 0));
        keyBindings.add(new KeyBind(Items.FLOWER_BANNER_PATTERN, new BindSetting("Experience Scroll", "Experience Scroll Key").visible(ServerUtil::isReallyWorld), 0));
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD, new BindSetting("Explosive Trap", "Explosive Trap Key").visible(ServerUtil::isHolyWorld), 5));
        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT, new BindSetting("Default Trap", "Default Trap Key").visible(ServerUtil::isHolyWorld), 0));
        keyBindings.add(new KeyBind(Items.NETHER_STAR, new BindSetting("Stun", "Stun Key").visible(ServerUtil::isHolyWorld), 30));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Explosive Thing", "Explosive Thing Key").visible(ServerUtil::isHolyWorld), 0));
        keyBindings.add(new KeyBind(Items.SNOWBALL, new BindSetting("SnowBall", "SnowBall Key").visible(() -> ServerUtil.isCopyTime() || ServerUtil.isHolyWorld()), 0));
        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE, new BindSetting("Gods Aura", "God's Aura Key").visible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, new BindSetting("Trap", "Trap Key").visible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.DRIED_KELP, new BindSetting("Plast", "Plast Key").visible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.SUGAR, new BindSetting("Clear Dust", "Clear Dust Key").visible(ServerUtil::isCopyTime), 10));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Fire Tornado", "Fire Tornado Key").visible(ServerUtil::isCopyTime), 10));
        keyBindings.add(new KeyBind(Items.ENDER_EYE, new BindSetting("Disorientation", "Disorientation Key").visible(ServerUtil::isCopyTime), 10));
        keyBindings.forEach(bind -> setup(bind.setting));
        setup(autoLootSetting, consumablesSetting, autoPointSetting, autoShulkerSetting, autoRepairSetting);
    }

    public ServerHelper() {
        super("ServerHelper", "Server Helper", ModuleCategory.MISC);
        initialize();
    }

    @Override
    public void activate() {
        script2.cleanup();
        stacks.clear();
    }


    @EventHandler
    public void onKey(KeyEvent e) {
        keyBindings.stream().filter(bind -> e.isKeyReleased(bind.setting.getKey()) && bind.setting.isVisible() && validDistance(bind.distance)).forEach(bind -> PlayerInventoryUtil.swapAndUse(bind.item));
    }


    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!PlayerIntersectionUtil.nullCheck()) switch (e.getPacket()) {
            case ItemPickupAnimationS2CPacket item when autoShulkerSetting.isValue() && autoShulkerSetting.isVisible() && item.getCollectorEntityId() == mc.player.getId() && mc.world.getEntityById(item.getEntityId()) instanceof ItemEntity entity -> {
                ItemStack stack = entity.getStack();
                if (stack.get(DataComponentTypes.CONTAINER) == null) {
                    stacks.put(-MathUtil.getRandom(1, 999999999), stack.getItem());
                    shulkerWatch.reset();
                }
            }
            case ScreenHandlerSlotUpdateS2CPacket slot -> {
                if (slot.getSyncId() == 0) {
                    Item item = slot.getStack().getItem();
                    stacks.entrySet().stream().filter(entry -> entry.getKey() < 0 && entry.getValue().equals(item)).findFirst().ifPresent(entry -> {
                        stacks.put(slot.getSlot() + 18, item);
                        stacks.remove(entry.getKey());
                    });
                }
            }
            case ChunkDeltaUpdateS2CPacket chunkDelta when consumablesSetting.isValue() && consumablesSetting.isVisible() -> {
                chunkDelta.visitUpdates((pos, state) -> blockStateMap.put(pos.add(0, 0, 0), state));
                script.addTickStep(0, () -> chunkDelta.visitUpdates((pos, state) -> {
                    Vec3d vec = pos.add(0, 0, 0).toCenterPos();
                    if (blockStateMap.size() > 50 && blockStateMap.size() < 600) {
                        if (isTrap(pos.up(2)))
                            addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 15000);
                        else if (isBigTrap(pos.up(3)))
                            addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 30000);
                    }
                }));
            }
            case GameMessageS2CPacket gameMessage when autoPointSetting.isValue() && autoPointSetting.isVisible() -> {
                Text content = gameMessage.content();
                String contentString = content.toString();
                String message = content.getString();
                String name = StringUtils.substringBetween(message, "|||   [", "]   ");
                if (name != null) {
                    String position = StringUtils.substringBetween(contentString, "value='/gps ", "'");
                    String lvl = StringUtils.substringBetween(message, "Уровень лута: ", "\n ║");
                    String owner = StringUtils.substringBetween(message, "Призван игроком: ", "\n ║");
                    if (position != null) {
                        String[] pose = position.split(" ");
                        Vec3d center = BlockPos.ofFloored(Integer.parseInt(pose[0]), Integer.parseInt(pose[1]), Integer.parseInt(pose[2])).toCenterPos();
                        switch (name) {
                            case "Мистический сундук" -> addEvent(name, lvl, owner, center, "overworld", 300, 0);
                            case "Вулкан" -> addEvent(name, lvl, owner, center, "overworld", 300, 120);
                            case "Метеоритный дождь", "Маяк убийца", "Мистический Алтарь" ->
                                    addEvent(name, lvl, owner, center, "overworld", 360, 0);
                            case "Загадочный маяк" -> addEvent(name, lvl, owner, center, "overworld", 60, 180);
                        }
                    } else {
                        switch (name) {
                            case "Сундук смерти" ->
                                    addEvent(name, lvl, owner, BlockPos.ofFloored(-155, 64, 205).toCenterPos(), "lobby", 300, 0);
                            case "Адская резня" ->
                                    addEvent(name, lvl, owner, BlockPos.ofFloored(48, 87, 73).toCenterPos(), "lobby", 180, 120);
                        }
                    }
                }
            }
            case GameMessageS2CPacket gameMessage -> {
                String message = gameMessage.content().getString();
                if (message.contains("▶ Повторно активировать Пузырь опыта возможно через")) {
                    String subString = StringUtils.substringBetween(message, "через ", " секунд");
                    if (subString != null && !subString.isEmpty()) {
                        int duration = Integer.parseInt(subString) * 20;
                        ItemCooldownManager manager = mc.player.getItemCooldownManager();
                        manager.set(Items.EXPERIENCE_BOTTLE.getDefaultStack(), duration);
                        CoolDowns.getInstance().packet(new PacketEvent(new CooldownUpdateS2CPacket(manager.getGroup(Items.EXPERIENCE_BOTTLE.getDefaultStack()), duration), PacketEvent.Type.RECEIVE));
                    }
                }
            }
            case OpenScreenS2CPacket openScreen when openScreen.getName().getString().contains("Рюкзак") && !stacks.isEmpty() ->
                    script.cleanup().addTickStep(0, script2::update);
            default -> {
            }
        }
    }


    @EventHandler
    public void onSetScreen(SetScreenEvent e) {
        if (e.getScreen() instanceof GenericContainerScreen screen && screen.getTitle().getString().contains("Рюкзак") && !script2.isFinished()) {
            e.setScreen(null);
        }
    }


    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            if (autoRepairSetting.isValue() && autoRepairSetting.isVisible() && StreamSupport.stream(mc.player.getArmorItems().spliterator(), false).anyMatch(stack -> {
                if ((double) stack.getDamage() / stack.getMaxDamage() < 0.94) return false;
                RegistryEntry<Enchantment> mendingEntry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.MENDING.getValue()).orElse(null);
                return mendingEntry != null && EnchantmentHelper.getLevel(mendingEntry, stack) > 0;
            })) PlayerInventoryUtil.slots().filter(slot -> {
                ItemStack stack = slot.getStack();
                NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
                return !mc.player.getItemCooldownManager().isCoolingDown(stack) && stack.getItem().equals(Items.EXPERIENCE_BOTTLE)
                        && component != null && component.toString().contains("\"text\":\" - при нажатие ПКМ, полностью ремонтирует\"") && repairWatch.every(5000);
            }).findFirst().ifPresent(slot -> PlayerInventoryComponent.addTask(() -> PlayerInventoryUtil.swapAndUse(slot, AngleUtil.cameraAngle())));
            if (!PlayerInventoryUtil.isServerScreen() && !stacks.isEmpty() && script2.isFinished() && shulkerWatch.finished(300)) {
                PlayerInventoryUtil.slots().filter(s -> s.getStack().get(DataComponentTypes.CONTAINER) != null)
                        .max(Comparator.comparingDouble(s -> s.getStack().getOrDefault(DataComponentTypes.CONTAINER, null).stacks.stream().filter(item -> !item.isEmpty()).toList().size()))
                        .ifPresent(shulker -> {
                            PlayerInventoryUtil.swapHand(shulker, Hand.MAIN_HAND, false);
                            PlayerInventoryUtil.closeScreen(false);
                            PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND);
                            script2.cleanup().addTickStep(0, () -> {
                                List<Integer> integers = new ArrayList<>();
                                PlayerInventoryUtil.slots().forEach(slot -> stacks.entrySet().stream().filter(entry -> slot.inventory.equals(mc.player.inventory) && entry.getValue().equals(slot.getStack().getItem()) && entry.getKey() == slot.id).forEach(entry -> {
                                    PlayerInventoryUtil.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
                                    integers.add(slot.id);
                                }));
                                integers.forEach(stacks::remove);
                                PlayerInventoryUtil.closeScreen(false);
                                PlayerInventoryUtil.swapHand(shulker, Hand.MAIN_HAND, false);
                                PlayerInventoryUtil.closeScreen(false);
                                shulkerWatch.reset();
                            });
                        });
            }
            if (autoLootSetting.isValue() && autoLootSetting.isVisible()) {
                PlayerIntersectionUtil.streamEntities().filter(MerchantEntity.class::isInstance).map(MerchantEntity.class::cast)
                        .filter(m -> m.hasStackEquipped(EquipmentSlot.MAINHAND) || m.hasStackEquipped(EquipmentSlot.OFFHAND)).findFirst().ifPresent(merchant -> {
                            Vec3d attackVector = pointFinder.computeVector(merchant, 6, RotationController.INSTANCE.getRotation(), new LinearSmoothMode().randomValue(), true).getLeft();
                            Angle angle = AngleUtil.calculateAngle(attackVector);

                            itemsWatch.reset();
                            entityUUID = merchant.getUuid();
                            if (mc.player.getEyePos().distanceTo(merchant.getBoundingBox().getCenter()) <= 6) {
                                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(merchant, false, Hand.MAIN_HAND, merchant.getBoundingBox().getCenter()));
                                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(merchant, false, Hand.MAIN_HAND));
                                RotationController.INSTANCE.rotateTo(angle, RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, this);
                            }
                        });
            }
            script.cleanupIfFinished().update();
            blockStateMap.clear();
            structures.removeIf(cons -> cons.time - System.currentTimeMillis() <= 0);
            serverEvents.removeIf(event -> event.timeEnd + 90000 - System.currentTimeMillis() <= 0);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        MatrixStack matrix = e.getStack();
        keyBindings.stream().filter(bind -> PlayerIntersectionUtil.isKey(bind.setting) && PlayerInventoryUtil.getSlot(bind.item) != null).forEach(bind -> {
            BlockPos playerPos = mc.player.getBlockPos();
            Vec3d smooth = MathUtil.interpolate(Vec3d.of(BlockPos.ofFloored(mc.player.prevX, mc.player.prevY, mc.player.prevZ)), Vec3d.of(playerPos)).subtract(Vec3d.of(playerPos));
            switch (bind.setting.getName()) {
                case "Trap", "Default Trap" -> drawItemCube(playerPos, smooth, 1.99F, ColorUtil.getClientColor());
                case "Disorientation", "Fire Tornado", "Clear Dust" ->
                        drawItemRadius(matrix, bind.distance, ColorUtil.LIGHT_RED);
                case "Explosive Thing" -> drawItemRadius(matrix, 5, ColorUtil.getClientColor());
                case "Plast" -> {
                    float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
                    if (Math.abs(mc.player.getPitch()) > 60) {
                        BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
                        Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3, true, true, true);
                    } else if (yaw <= -157.5F || yaw >= 157.5F) {
                        BlockPos blockPos = playerPos.north(3).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3, true, true, true);
                    } else if (yaw <= -112.5F) {
                        drawSidePlast(playerPos.east(5).south().down(), smooth, ColorUtil.getClientColor(), -1, true);
                    } else if (yaw <= -67.5F) {
                        BlockPos blockPos = playerPos.east(2).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3, true, true, true);
                    } else if (yaw <= -22.5F) {
                        drawSidePlast(playerPos.east(5).down(), smooth, ColorUtil.getClientColor(), 1, false);
                    } else if (yaw >= -22.5 && yaw <= 22.5) {
                        BlockPos blockPos = playerPos.south(2).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3, true, true, true);
                    } else if (yaw <= 67.5F) {
                        drawSidePlast(playerPos.west(4).down(), smooth, ColorUtil.getClientColor(), 1, true);
                    } else if (yaw <= 112.5F) {
                        BlockPos blockPos = playerPos.west(3).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3, true, true, true);
                    } else if (yaw <= 157.5F) {
                        drawSidePlast(playerPos.west(4).south().down(), smooth, ColorUtil.getClientColor(), -1, false);
                    }
                }
                case "Explosive Trap" -> drawItemCube(playerPos, smooth, 3.99F, ColorUtil.LIGHT_RED);
                case "Stun" -> drawItemCube(playerPos, smooth, 15.01F, ColorUtil.LIGHT_RED);
                case "SnowBall" ->
                        ProjectilePrediction.getInstance().drawPredictionInHand(matrix, List.of(Items.SNOWBALL.getDefaultStack()), AngleUtil.cameraAngle());
            }
        });
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();

        structures.forEach(cons -> {
            double time = (cons.time - System.currentTimeMillis()) / 1000;
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(cons.vec);

            String text = MathUtil.round(time, 0.1F) + "с";
            FontRenderer font = Fonts.getSize(14);
            float width = font.getStringWidth(text);
            float posX = (float) (vec3d.x - width / 2);
            float posY = (float) vec3d.y;
            float padding = 2;

            if (ProjectionUtil.canSee(cons.vec) && cons.anarchy == ServerUtil.getAnarchy() && ServerUtil.getWorldType().equals(cons.world)) {
                blur.render(ShapeProperties.create(matrix, posX - padding, posY - padding, width + padding * 2, 10)
                        .round(1.5F).color(ColorUtil.HALF_BLACK).build());
                font.drawString(matrix, text, posX, posY + 1, ColorUtil.getText());

                Render2DUtil.defaultDrawStack(context, cons.item.getDefaultStack(), posX - 14, posY - 2.5F, true, false, 0.5F);
            }
        });
        serverEvents.forEach(event -> {
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(event.vec);

            double timeOpen = (event.timeOpen - System.currentTimeMillis()) / 1000;
            double timeEnd = (event.timeEnd - System.currentTimeMillis()) / 1000;
            String distance = " [" + MathUtil.round(mc.getEntityRenderDispatcher().camera.getPos().distanceTo(event.vec), 0.1) + "m" + "]";
            String time = timeOpen > 0 ? ("До начала: " + MathUtil.round(timeOpen, timeOpen < 30 ? 0.1F : 1) + "с").replace(".0", "")
                    : timeEnd > 0 ? ("До конца: " + MathUtil.round(timeEnd, timeEnd < 30 ? 0.1F : 1) + "с").replace(".0", "")
                    : "Конец ивента!";

            if (ProjectionUtil.canSee(event.vec) && event.anarchy == ServerUtil.getAnarchy() && ServerUtil.getWorldType().equals(event.world)) {
                List<String> list = new ArrayList<>(Collections.singletonList(event.name + distance));
                if (event.owner != null) list.add("Призван: " + Formatting.GOLD + event.owner);
                list.add(time);
                if (event.lvl != null) list.add(event.lvl);
                draw(matrix, Fonts.getSize(14), list, vec3d);
            }
        });
        PlayerIntersectionUtil.streamEntities().filter(ent -> ent.getUuid().equals(entityUUID)).forEach(ent -> {
            Vec3d pos = ent.getBlockPos().down().toCenterPos();
            Vec3d vec = ProjectionUtil.worldSpaceToScreenSpace(pos);

            String text = !itemsWatch.finished(200) ? "Можно забрать" : !itemsWatch.finished(20000) ? MathUtil.round(20 - itemsWatch.elapsedTime() / 1000F, 0.1F) + "с" : "Скоро";
            FontRenderer font = Fonts.getSize(14);
            float height = 4;
            float width = font.getStringWidth(text);
            float padding = 3;
            double x = vec.getX() - width / 2;
            double y = vec.getY() - height / 2;
            Formatting formatting = mc.player.getEyePos().distanceTo(ent.getEyePos()) < 5F ? Formatting.GREEN : Formatting.RED;

            if (ProjectionUtil.canSee(pos)) {
                blur.render(ShapeProperties.create(matrix, x - padding, y - padding, width + padding * 2, height + padding * 2)
                        .round(2).color(ColorUtil.HALF_BLACK).build());

                font.drawString(matrix, formatting + text, x, y, ColorUtil.getText());
            }
        });
    }

    private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int color) {
        Box box = new Box(playerPos.up()).offset(smooth).expand(size);
        boolean inBox = mc.world.getPlayers().stream().map(player -> SimulatedPlayer.simulateOtherPlayer(player, 2))
                .anyMatch(simulated -> simulated.player != mc.player && box.intersects(simulated.boundingBox) && !FriendUtils.isFriend(simulated.player));
        Render3DUtil.drawBox(box, inBox ? ColorUtil.getFriendColor() : color, 3, true, true, true);
    }

    private void drawItemRadius(MatrixStack matrix, float distance, int clr) {
        float playerHalfWidth = mc.player.getWidth() / 2;
        int color = validDistance(distance) ? ColorUtil.getFriendColor() : clr;
        Vec3d pos = MathUtil.interpolate(mc.player).add(playerHalfWidth, 0.02, playerHalfWidth);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = MathUtil.cosSin(i, size, distance);
            Vec3d nextCosSin = MathUtil.cosSin(i + 1, size, distance);
            Render3DUtil.vertexLine(matrix, buffer, pos.add(cosSin), pos.add(cosSin.x, cosSin.y + 2, cosSin.z), ColorUtil.multAlpha(color, 0.2F), ColorUtil.multAlpha(color, 0));
            Render3DUtil.drawLine(pos.add(cosSin), pos.add(nextCosSin), color, 2, true);
        }
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = MathUtil.cosSin(i, size, distance);
            Render3DUtil.vertexLine(matrix, buffer, pos.add(cosSin), pos.add(cosSin.x, cosSin.y - 2, cosSin.z), ColorUtil.multAlpha(color, 0.2F), ColorUtil.multAlpha(color, 0));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    private void draw(MatrixStack matrix, FontRenderer font, List<String> list, Vec3d vec3d) {
        float offsetY = 0;
        for (int i = 0; i < list.size(); i++) {
            String string = list.get(i);
            float width = font.getStringWidth(string);
            float posX = (float) (vec3d.x - width / 2);

            blur.render(ShapeProperties.create(matrix, posX - 2, vec3d.y - 2 + offsetY, width + 2 * 2, 10)
                    .softness(3).round(getRound(font, list, i, width)).color(ColorUtil.HALF_BLACK).build());
            font.drawString(matrix, string, posX, vec3d.y + 1 + offsetY, ColorUtil.getText());

            offsetY += 10;
        }
    }

    public void drawSidePlast(BlockPos blockPos, Vec3d smooth, int color, int i, boolean ff) {
        Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
        float width = 2;
        int quadColor = ColorUtil.multAlpha(color, 0.15F);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawVerticalLines(vec3d, color, width, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawVerticalQuads(vec3d, quadColor, i, ff);
    }

    private void drawHorizontalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(x, 0, 0), color, width, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(0, 0, i), color, width, true);
            Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(x, 0, 0), color, width, true);
        }
        Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(0, 0, i), color, width, true);
        Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(x * -2, 0, 0), color, width, true);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(0, 0, i * -1), color, width, true);
            Render3DUtil.drawLine(vec3d, vec3d = vec3d.add(x * -1, 0, 0), color, width, true);
        }
        Render3DUtil.drawLine(vec3d, vec3d.add(0, 0, i * -2), color, width, true);
    }

    private void drawVerticalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawLine(vec3d, vec3d.add(0, 5, 0), color, width, true);
        Render3DUtil.drawLine(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 5, 0), color, width, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawLine(vec3d = vec3d.add(x, 0, i), vec3d.add(0, 5, 0), color, width, true);
        }
        Render3DUtil.drawLine(vec3d = vec3d.add(0, 0, i), vec3d.add(0, 5, 0), color, width, true);
        Render3DUtil.drawLine(vec3d = vec3d.add(x * -2, 0, 0), vec3d.add(0, 5, 0), color, width, true);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawLine(vec3d = vec3d.add(x * -1, 0, i * -1), vec3d.add(0, 5, 0), color, width, true);
        }
    }

    private void drawHorizontalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        vec3d = vec3d.add(0, 1e-3, 0);
        float x = ff ? i : -i;
        Render3DUtil.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        for (int f = 0; f < 3; f++)
            Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i), vec3d.add(0, 0, i), color, true);
    }

    private void drawVerticalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
            Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
        Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x * -2, 0, 0), vec3d.add(x * -2, 5, 0), vec3d.add(0, 5, 0), color, true);
        vec3d = vec3d.add(x * -1, 0, 0);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -1), vec3d.add(0, 5, i * -1), vec3d.add(0, 5, 0), color, true);
            Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i * -1), vec3d.add(x * -1, 0, 0), vec3d.add(x * -1, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3DUtil.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -2), vec3d.add(0, 5, i * -2), vec3d.add(0, 5, 0), color, true);
    }

    private void addEvent(String name, String lvl, String owner, Vec3d vec3d, String world, int timeOpen, int timeLoot) {
        if (serverEvents.stream().noneMatch(server -> server.vec.equals(vec3d))) {
            long open = System.currentTimeMillis() + timeOpen * 1000L;
            long loot = open + timeLoot * 1000L;
            serverEvents.add(new ServerEvent(name, lvl, owner, vec3d, world, ServerUtil.getAnarchy(), open, loot));
        }
    }

    private void addStructure(Item item, Vec3d vec, double time) {
        if (structures.stream().noneMatch(str -> str.vec.equals(vec))) {
            structures.add(new Structure(item, vec, ServerUtil.getWorldType(), ServerUtil.getAnarchy(), time));
        }
    }

    private Vector4f getRound(FontRenderer font, List<String> list, int i, float width) {
        if (i == 0) {
            float next = font.getStringWidth(list.get(i + 1));
            return next >= width ? new Vector4f(2, 0, 2, 0) : new Vector4f(2);
        }
        if (i == list.size() - 1) {
            float prev = font.getStringWidth(list.get(i - 1));
            return prev >= width ? new Vector4f(0, 2, 0, 2) : new Vector4f(2);
        }
        float prev = font.getStringWidth(list.get(i - 1));
        float next = font.getStringWidth(list.get(i + 1));
        return prev >= width ? next >= width ? new Vector4f() : new Vector4f(0, 2, 0, 2) : new Vector4f(2);
    }

    private boolean validDistance(float dist) {
        return dist == 0 || mc.world.getPlayers().stream().anyMatch(p -> p != mc.player && !FriendUtils.isFriend(p) && mc.player.distanceTo(p) <= dist);
    }

    private boolean isTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerIntersectionUtil.getCube(center, 2)) {
            if (pos.toCenterPos().distanceTo(center.toCenterPos()) < 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(2).north().east()) && !pos.equals(center.up(2).north().west()) && !pos.equals(center.up(2).south().east()) && !pos.equals(center.up(2).south().west())) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    private boolean isBigTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerIntersectionUtil.getCube(center, 3)) {
            if (Math.abs(pos.getX() - center.getX()) <= 2 && Math.abs(pos.getY() - center.getY()) <= 2 && Math.abs(pos.getZ() - center.getZ()) <= 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(3))) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    public record KeyBind(Item item, BindSetting setting, float distance) {
    }

    public record Structure(Item item, Vec3d vec, String world, int anarchy, double time) {
    }

    public record ServerEvent(String name, String lvl, String owner, Vec3d vec, String world, int anarchy,
                              double timeOpen, double timeEnd) {
    }
}