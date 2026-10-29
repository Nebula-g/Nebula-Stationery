package com.nebula.stationery.mq.handler;

import com.nebula.stationery.mq.Message;
import com.nebula.stationery.mq.MessageHandler;
import com.nebula.stationery.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 新评价落库后异步刷新商品缓存，让详情页的评分和列表尽早更新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewCreatedHandler implements MessageHandler {

    public static final String TYPE = "review.created";

    private final ProductService productService;

    @Override
    public String supportedType() {
        return TYPE;
    }

    @Override
    public void handle(Message message) {
        long productId = message.longField("productId");
        productService.evictProductCache(productId);
        log.info("收到新评价，已清除商品 {} 的缓存", productId);
    }
}
