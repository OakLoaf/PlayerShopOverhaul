package me.xemor.playershopoverhaul.storage;

public class ExceedsMaxPriceException extends Exception {
    public ExceedsMaxPriceException(String message) {
        super(message);
    }
}
