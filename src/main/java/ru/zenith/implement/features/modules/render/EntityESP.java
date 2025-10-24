package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.*;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.api.repository.friend.FriendUtils;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.world.ServerUtil;
import ru.zenith.common.util.math.ProjectionUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.DrawEvent;
import ru.zenith.implement.events.render.WorldLoadEvent;
import ru.zenith.implement.features.modules.combat.AntiBot;

import java.lang.Math;
import java.util.*;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntityESP extends Module {
    public static EntityESP getInstance() {
        return Instance.get(EntityESP.class);
    }

    Identifier TEXTURE = Identifier.of("textures/container.png");
    List<CustomPlayer> players = new ArrayList<>();

    ValueSetting sizeSetting = new ValueSetting("Tag Size", "Tags size")
            .setValue(13).range(10, 20);
    public MultiSelectSetting entityType = new MultiSelectSetting("Entity Type", "Entity that will be displayed")
            .value("Player","Item","TNT");
    MultiSelectSetting playerSetting = new MultiSelectSetting("Player Settings", "Settings for players")
            .value("Flat Box", "Armor", "Prefix", "Hand Items").visible(()-> entityType.isSelected("Player"));

    public EntityESP() {
        super("EntityESP", "Entity ESP", ModuleCategory.RENDER);
        setup(sizeSetting, entityType, playerSetting);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.removeIf(p -> p.animation.isFinished(Direction.FORWARDS));
        players.forEach(p -> {p.player.prevX = p.player.getX();p.player.prevY = p.player.getY();p.player.prevZ = p.player.getZ();});
        mc.world.getPlayers().stream().filter(player -> player != mc.player).forEach(player -> {
            players.removeIf(s -> s.player.getId() == player.getId());
            players.add(new CustomPlayer(player, new DecelerateAnimation().setMs(1000).setValue(1)));
        });
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(sizeSetting.getInt(), Fonts.Type.DEFAULT);
        FontRenderer bigFont = Fonts.getSize(sizeSetting.getInt() + 2, Fonts.Type.DEFAULT);
        if (entityType.isSelected("Player")) {
            for (CustomPlayer customEntity : players) {
                PlayerEntity player = customEntity.player;
                if (player == null) continue;

                Vector4d vec4d = ProjectionUtil.getVector4D(player);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(player.getBoundingBox().getCenter());
                float animRemove = MathHelper.clamp(1 - customEntity.animation.getOutput().floatValue(),0,1);
                boolean friend = FriendUtils.isFriend(player);

                if (distance < 1) continue;
                if (ProjectionUtil.cantSee(vec4d)) continue;

                if (playerSetting.isSelected("Flat Box")) drawFlatBox(friend, vec4d);
                if (playerSetting.isSelected("Armor")) drawArmor(context, player, vec4d);
                if (playerSetting.isSelected("Hand Items")) drawHands(matrix, player, font, vec4d);
                drawText(matrix, getTextPlayer(player, friend), ProjectionUtil.centerX(vec4d), vec4d.y - 3, font);
            }
        }
        List<Entity> entities = PlayerIntersectionUtil.streamEntities()
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .toList();
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity item && entityType.isSelected("Item")) {
                Vector4d vec4d = ProjectionUtil.getVector4D(entity);
                ItemStack stack = item.getStack();
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();

                if (ProjectionUtil.cantSee(vec4d)) continue;

                Text text = item.getStack().getName();
                if (stack.getCount() > 1) text = text.copy().append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");

                if (!list.isEmpty()) drawShulkerBox(context, stack, list, vec4d);
                else drawText(matrix, text, ProjectionUtil.centerX(vec4d), vec4d.y, text.getContent().toString().equals("empty") ? bigFont : font);
            } else if (entity instanceof TntEntity tnt && entityType.isSelected("TNT")) {
                Vector4d vec4d = ProjectionUtil.getVector4D(entity);
                if (ProjectionUtil.cantSee(vec4d)) continue;
                drawText(matrix, tnt.getStyledDisplayName(), ProjectionUtil.centerX(vec4d), vec4d.y, font);
            }
        }
    }

    private void drawFlatBox(boolean friend, Vector4d vec) {
        int client = friend ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
        int black = ColorUtil.HALF_BLACK;

        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 3;

        Render2DUtil.drawQuad(posX - 1F, posY - 1,size + 1,1.5F,black);
        Render2DUtil.drawQuad(posX - 1F, posY + 0.5F,1.5F,size + 0.5F,black);
        Render2DUtil.drawQuad(posX - 1F, endPosY - size - 1,1.5F,size,black);
        Render2DUtil.drawQuad(posX - 1F, endPosY - 1,size + 1,1.5F,black);
        Render2DUtil.drawQuad(endPosX - size + 0.5F, posY - 1,size + 1,1.5F,black);
        Render2DUtil.drawQuad(endPosX, posY + 0.5F,1.5F,size + 0.5F,black);
        Render2DUtil.drawQuad(endPosX, endPosY - size - 1,1.5F,size,black);
        Render2DUtil.drawQuad(endPosX - size + 0.5F, endPosY - 1,size + 1,1.5F,black);

        Render2DUtil.drawQuad(posX - 0.5F, posY - 0.5F,size,0.5F,client);
        Render2DUtil.drawQuad(posX - 0.5F, posY,0.5F,size + 0.5F,client);
        Render2DUtil.drawQuad(posX - 0.5F, endPosY - size - 0.5F,0.5F,size,client);
        Render2DUtil.drawQuad(posX - 0.5F, endPosY - 0.5F,size,0.5F,client);
        Render2DUtil.drawQuad(endPosX - size + 1, posY - 0.5F,size,0.5F,client);
        Render2DUtil.drawQuad(endPosX + 0.5F, posY,0.5F,size + 0.5F,client);
        Render2DUtil.drawQuad(endPosX + 0.5F, endPosY - size - 0.5F,0.5F,size,client);
        Render2DUtil.drawQuad(endPosX - size + 1, endPosY - 0.5F,size,0.5F,client);
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec) {
        MatrixStack matrix = context.getMatrices();
        List<ItemStack> items = new ArrayList<>();
        player.getEquippedItems().forEach(s -> {if (!s.isEmpty()) items.add(s);});

        float posX = (float) (ProjectionUtil.centerX(vec) - items.size() * 5.5);
        float posY = (float) (vec.y - sizeSetting.getInt() / 1.5 - 15);
        float padding = 0.5F;
        float offset = -11;

        if (!items.isEmpty()) {
            matrix.push();
            matrix.translate(posX, posY, 0);
            blur.render(ShapeProperties.create(matrix, -padding, -padding,items.size() * 11 - 1 + padding * 2,10 + padding * 2)
                    .round(2.5F).color(ColorUtil.HALF_BLACK).build());
            for (ItemStack stack : items) {
                Render2DUtil.defaultDrawStack(context, stack, offset += 11, 0, false, false, 0.5F);
            }
            matrix.pop();
        }
    }

    private void drawHands(MatrixStack matrix, PlayerEntity player, FontRenderer font, Vector4d vec) {
        double posY = vec.w;
        for (ItemStack stack : player.getHandItems()) {
            if (stack.isEmpty()) continue;

            MutableText text = Text.empty().append(stack.getName());

            if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
            posY += font.getStringHeight(text) / 2 + 3;
            drawText(matrix,text,ProjectionUtil.centerX(vec),posY,font);
        }
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        MatrixStack matrix = context.getMatrices();
        int width = 176;
        int height = 67;
        int color = ColorUtil.multBright(ColorUtil.replAlpha(((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color, 1F), 1);

        matrix.push();
        matrix.translate(ProjectionUtil.centerX(vec) - (double) width / 4, vec.w + 2, -200 + Math.cos(vec.x));
        matrix.scale(0.5F, 0.5F, 1);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0, 0, width, height, width, height, color);

        int posX = 7;
        int posY = 6;

        for (ItemStack stack : stacks.stream().toList()) {
            Render2DUtil.defaultDrawStack(context, stack, posX, posY, false, true, 1);
            posX += 18;
            if (posX >= 165) {
                posY += 18;
                posX = 7;
            }
        }
        matrix.pop();
    }

    private void drawText(MatrixStack matrix, Text text, double startX, double startY, FontRenderer font) {
        int paddingX = 2;
        float paddingY = 0.75F;
        float height = font.getFont().getSize() / 1.5F;
        float width = font.getStringWidth(text);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;

        blur.render(ShapeProperties.create(matrix, posX - paddingX, posY - paddingY, width + paddingX * 2, height + paddingY * 2)
                .round(height / 4).color(ColorUtil.HALF_BLACK).build());
        font.drawText(matrix, text, posX, posY + 3);
    }

    private MutableText getTextPlayer(PlayerEntity player, boolean friend) {
        float health = PlayerIntersectionUtil.getHealth(player);
        Item offHandItem = player.getOffHandStack().getItem();
        MutableText text = Text.empty();
        if (friend) text.append("[" + Formatting.GREEN + "F" + Formatting.RESET + "] ");
        if (AntiBot.getInstance().isBot(player)) text.append("[" + Formatting.DARK_RED + "BOT" + Formatting.RESET + "] ");
        if (playerSetting.isSelected("Prefix")) text.append(player.getDisplayName()); else text.append(player.getName());
        if (offHandItem.equals(Items.PLAYER_HEAD) || offHandItem.equals(Items.TOTEM_OF_UNDYING)) text.append(Formatting.RESET + getSphere(player.getOffHandStack()));
        if (health >= 0 && health <= player.getMaxHealth()) text.append(Formatting.RESET + " [" + Formatting.RED + PlayerIntersectionUtil.getHealthString(player) + Formatting.RESET + "]");
        return text;
    }

    private String getSphere(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (ServerUtil.isFunTime() && component != null) {
            NbtCompound compound = component.copyNbt();
            if (compound.getInt("tslevel") != 0) {
                return " [" + Formatting.GOLD + compound.getString("don-item").replace("sphere-", "").toUpperCase() + Formatting.RESET + "]";
            }
        }
        return "";
    }

    public record CustomPlayer(PlayerEntity player, Animation animation) {}
}