package com.example.DunbarHorizon.social.application.port.out;

public interface SocialNetworkCacheManager {
    void evictDefaultNetwork(Long userId);
    void evictLabelNetwork(Long userId, String labelId);
    void evictAllLabelNetworks(Long userId);
}
