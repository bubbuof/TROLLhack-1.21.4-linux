package ru.zenith.common.util.auction;

import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class AuctionPriceParser {
    private final Pattern funTimePricePattern = Pattern.compile("\\$(\\d+(?:\\s\\d{3})*(?:\\.\\d{2})?)");

    public int getPrice(ItemStack stack) {
        ComponentMap tag = stack.getComponents();
        if (tag == null) return -1;
        String price = StringUtils.substringBetween(tag.toString(), "literal{ $", "}[style={color=green}]");
        if (price == null || price.isEmpty()) return -1;
        price = price.replaceAll(" ", "").replaceAll(",", "");
        return Integer.parseInt(price);
    }
}