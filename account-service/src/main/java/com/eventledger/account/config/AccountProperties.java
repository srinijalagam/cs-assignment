package com.eventledger.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account")
public class AccountProperties {

    private int recentTransactionLimit = 10;

    public int getRecentTransactionLimit() {
        return recentTransactionLimit;
    }

    public void setRecentTransactionLimit(int recentTransactionLimit) {
        this.recentTransactionLimit = recentTransactionLimit;
    }
}
