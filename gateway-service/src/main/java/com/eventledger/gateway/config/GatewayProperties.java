package com.eventledger.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private final AccountService accountService = new AccountService();
    private int recentEventLimit = 10;

    public AccountService getAccountService() {
        return accountService;
    }

    public int getRecentEventLimit() {
        return recentEventLimit;
    }

    public void setRecentEventLimit(int recentEventLimit) {
        this.recentEventLimit = recentEventLimit;
    }

    public static class AccountService {
        private String baseUrl = "http://localhost:8081";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
