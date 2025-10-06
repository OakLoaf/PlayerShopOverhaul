package me.xemor.playershopoverhaul.configuration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LanguageConfig {
    @JsonProperty
    private List<String> help;
    @JsonProperty
    @JsonAlias("gts-disabled")
    private String gtsDisabled;
    @JsonProperty
    private SellConfig sell;
    @JsonProperty
    private ClaimConfig claim;
    @JsonProperty
    private ListingConfig listing;
    @JsonProperty
    private GTSViewConfig GTSView;
    @JsonProperty
    private ItemPurchaseViewConfig itemPurchaseView;

    public List<String> getHelp() { return help; }

    public String getGtsDisabled() { return gtsDisabled; }

    public SellConfig getSell() { return sell; }

    public ClaimConfig getClaim() { return claim; }

    public ListingConfig getListing() { return listing; }

    public GTSViewConfig getGTSView() { return GTSView; }

    public ItemPurchaseViewConfig getItemPurchaseView() {
        return itemPurchaseView;
    }

    public static class SellConfig {
        @JsonProperty
        private String soldmessage;

        public String getSoldMessage() { return soldmessage; }
    }

    public static class ClaimConfig {
        @JsonProperty
        private String claimed;

        public String getClaimed() { return claimed; }
    }

    public static class ListingConfig {
        @JsonProperty
        private String name;
        @JsonProperty
        private List<String> lore;

        public String getName() { return name; }

        public List<String> getLore() { return lore; }
    }

    public static class GTSViewConfig {
        @JsonProperty
        private String title;
        @JsonProperty
        private ItemStack forwardArrow;
        @JsonProperty
        private ItemStack backArrow;
        @JsonProperty
        private ItemStack refresh;
        @JsonProperty
        private ItemStack listings;
        @JsonProperty
        private ItemStack menuBackButton;
        @JsonProperty
        private ItemStack search;

        public String getTitle() {
            return title;
        }

        public ItemStack getForwardArrow() {
            return forwardArrow;
        }

        public ItemStack getBackArrow() {
            return backArrow;
        }

        public ItemStack getRefresh() {
            return refresh;
        }

        public ItemStack getListings() {
            return listings;
        }

        public ItemStack getMenuBackButton() {
            return menuBackButton;
        }

        public ItemStack getSearch() {
            return search;
        }
    }

    public static class ItemPurchaseViewConfig {
        @JsonProperty
        private String title;
        @JsonProperty
        private ListingConfig listing;
        @JsonProperty
        private ItemStack menuBackButton;
        @JsonProperty
        private ItemStack purchase64Less;
        @JsonProperty
        private ItemStack purchase16Less;
        @JsonProperty
        private ItemStack purchase1Less;
        @JsonProperty
        private ItemStack purchase1More;
        @JsonProperty
        private ItemStack purchase16More;
        @JsonProperty
        private ItemStack purchase64More;
        @JsonProperty
        private ItemStack purchase192More;

        public String getTitle() {
            return title;
        }

        public ListingConfig getListing() {
            return listing;
        }

        public ItemStack getMenuBackButton() {
            return menuBackButton;
        }

        public ItemStack getPurchase64Less() {
            return purchase64Less;
        }

        public ItemStack getPurchase16Less() {
            return purchase16Less;
        }

        public ItemStack getPurchase1Less() {
            return purchase1Less;
        }

        public ItemStack getPurchase1More() {
            return purchase1More;
        }

        public ItemStack getPurchase16More() {
            return purchase16More;
        }

        public ItemStack getPurchase64More() {
            return purchase64More;
        }

        public ItemStack getPurchase192More() {
            return purchase192More;
        }
    }
}
