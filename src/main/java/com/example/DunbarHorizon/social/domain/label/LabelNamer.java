package com.example.DunbarHorizon.social.domain.label;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LabelNamer {

    public LabelName assign(String requestedName, Set<String> occupiedNames) {
        LabelName original = new LabelName(requestedName);
        if (!occupiedNames.contains(requestedName)) {
            return original;
        }

        for (long number = 2; ; number++) {
            String suffix = " (" + number + ")";
            int prefixLength = Math.min(requestedName.length(), LabelName.MAX_LENGTH - suffix.length());
            if (prefixLength < requestedName.length()
                    && Character.isSurrogatePair(requestedName.charAt(prefixLength - 1), requestedName.charAt(prefixLength))) {
                prefixLength--;
            }
            String candidate = requestedName.substring(0, prefixLength) + suffix;
            if (!occupiedNames.contains(candidate)) {
                return new LabelName(candidate);
            }
        }
    }
}
