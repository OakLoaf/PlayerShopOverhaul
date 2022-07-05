package me.xemor.playershopoverhaul.userinterface;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GTSData {

    @NotNull
    private String currentSearch;
    private int pageNumber;

    public GTSData(@NotNull String currentSearch, int pageNumber) {
        this.currentSearch = currentSearch;
        this.pageNumber = pageNumber;
    }

    @NotNull
    public String getCurrentSearch() {
        return currentSearch;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setCurrentSearch(@Nullable String currentSearch) {
        this.currentSearch = currentSearch;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}
