package com.hamtech.bookstorepromotionservice.messaging;

public final class PromotionRoutingKeys {

    public static final String RESERVE_COMMAND = "promotion.reserve.command";
    public static final String CONFIRM_COMMAND = "promotion.confirm.command";
    public static final String RELEASE_COMMAND = "promotion.release.command";

    public static final String RESERVED_EVENT = "promotion.reserved";
    public static final String CONFIRMED_EVENT = "promotion.confirmed";
    public static final String RELEASED_EVENT = "promotion.released";
    public static final String FAILED_EVENT = "promotion.failed";

    private PromotionRoutingKeys() {
    }
}
