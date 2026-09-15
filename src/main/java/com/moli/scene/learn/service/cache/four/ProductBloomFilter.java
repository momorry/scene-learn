package com.moli.scene.learn.service.cache.four;

import cn.hutool.bloomfilter.BitSetBloomFilter;
import org.springframework.stereotype.Component;

@Component
public class ProductBloomFilter {

    public void initBloomFilter() {
        BitSetBloomFilter bitSetBloomFilter = new BitSetBloomFilter(1024, 10, 1);
        for (int i = 0; i < 1000000; i++) {
            bitSetBloomFilter.add("" + i);
        }
    }
}
