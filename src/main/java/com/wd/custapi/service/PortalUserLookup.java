package com.wd.custapi.service;

public interface PortalUserLookup {
    View lookup(Long portalUserId);

    record View(Long userId, String name, String phone, String email, String photoUrl) {}
}
