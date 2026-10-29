package com.nebula.stationery.service;

import com.nebula.stationery.entity.SensitiveWord;
import com.nebula.stationery.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 敏感词过滤：DFA 有限状态自动机。
 * 词库缓存在内存中，管理员增删敏感词后调用 reload() 重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private static final char MASK = '*';
    private static final String END_FLAG = "\0";

    private final SensitiveWordMapper sensitiveWordMapper;

    private final AtomicReference<Map<Character, Object>> wordTree = new AtomicReference<>(new HashMap<>());

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        List<SensitiveWord> words = sensitiveWordMapper.selectList(null);
        Map<Character, Object> tree = new HashMap<>();
        for (SensitiveWord sw : words) {
            addWord(tree, sw.getWord());
        }
        wordTree.set(tree);
        log.info("敏感词库已加载，共 {} 个词", words.size());
    }

    @SuppressWarnings("unchecked")
    private void addWord(Map<Character, Object> tree, String word) {
        if (word == null || word.isEmpty()) return;
        Map<Character, Object> current = tree;
        for (char c : word.toCharArray()) {
            Object next = current.get(c);
            if (next == null) {
                next = new HashMap<Character, Object>();
                current.put(c, next);
            }
            current = (Map<Character, Object>) next;
        }
        current.put(END_FLAG.charAt(0), Boolean.TRUE);
    }

    /** 命中的敏感词列表，用于提示用户 */
    @SuppressWarnings("unchecked")
    public List<String> findSensitiveWords(String text) {
        List<String> hits = new ArrayList<>();
        if (text == null || text.isEmpty()) return hits;

        Map<Character, Object> tree = wordTree.get();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            Map<Character, Object> current = tree;
            int matchedLength = 0;
            int lastValidEnd = -1;

            for (int j = i; j < length; j++) {
                char c = text.charAt(j);
                Object node = current.get(c);
                if (node == null) break;

                matchedLength++;
                current = (Map<Character, Object>) node;
                if (Boolean.TRUE.equals(current.get(END_FLAG.charAt(0)))) {
                    lastValidEnd = matchedLength;
                }
            }

            if (lastValidEnd > 0) {
                hits.add(text.substring(i, i + lastValidEnd));
                i += lastValidEnd - 1;
            }
        }
        return hits;
    }

    /** 将命中的敏感词替换为等长星号 */
    public String filter(String text) {
        if (text == null || text.isEmpty()) return text;
        List<String> hits = findSensitiveWords(text);
        if (hits.isEmpty()) return text;

        StringBuilder sb = new StringBuilder(text);
        for (String hit : hits) {
            int from = 0;
            String mask = String.valueOf(MASK).repeat(hit.length());
            while ((from = sb.indexOf(hit, from)) >= 0) {
                sb.replace(from, from + hit.length(), mask);
                from += hit.length();
            }
        }
        return sb.toString();
    }

    public boolean containsSensitiveWord(String text) {
        return !findSensitiveWords(text).isEmpty();
    }
}
