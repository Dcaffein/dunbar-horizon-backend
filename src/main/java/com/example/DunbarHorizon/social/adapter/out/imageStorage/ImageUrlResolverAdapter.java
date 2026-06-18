package com.example.DunbarHorizon.social.adapter.out.imageStorage;

import com.example.DunbarHorizon.global.imageStorage.S3ImageUrlResolver;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUrlResolverAdapter implements ImageUrlResolverPort {

    private final S3ImageUrlResolver s3ImageUrlResolver;

    @Override
    public String resolveUrl(String key) {
        return s3ImageUrlResolver.resolveUrl(key);
    }
}
