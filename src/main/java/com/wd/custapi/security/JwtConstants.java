package com.wd.custapi.security;

/**
 * Shared JWT constants used across the authentication layer.
 * These values must stay in sync with the Portal API's equivalent constants.
 * Any change here requires a corresponding change in wd_portal_api.
 */
public final class JwtConstants {
    private JwtConstants() {}

    /** Claim key for token type differentiation (PORTAL, CUSTOMER, PARTNER) */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    /** Token type for portal staff users */
    public static final String TOKEN_TYPE_PORTAL = "PORTAL";

    /** Token type for partner users */
    public static final String TOKEN_TYPE_PARTNER = "PARTNER";

    /** Token type for customer users */
    public static final String TOKEN_TYPE_CUSTOMER = "CUSTOMER";

    /** Claim key for audience validation */
    public static final String CLAIM_AUDIENCE = "aud";

    /** Portal API audience value */
    public static final String AUDIENCE_PORTAL = "portal-api";

    /** Customer API audience value */
    public static final String AUDIENCE_CUSTOMER = "customer-api";

    /** Default token type when claim is missing (legacy tokens) */
    public static final String DEFAULT_TOKEN_TYPE = TOKEN_TYPE_CUSTOMER;
}
