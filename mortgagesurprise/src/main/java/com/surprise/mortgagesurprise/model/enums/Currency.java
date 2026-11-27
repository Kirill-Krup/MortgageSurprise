package com.surprise.mortgagesurprise.model.enums;

public enum Currency {
    USD("$"),
    EUR("€"),
    BYN("Br"),
    RUB("₽");

    private final String symbol;

    Currency(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
