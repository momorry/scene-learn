package com.moli.scene.learn.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheDeleteMessage implements Serializable {
    private String messageId;
    private String cacheKey;
}