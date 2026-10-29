package com.nebula.stationery.sms;

/**
 * 短信通道抽象：默认走模拟实现，把验证码打到日志里；
 * 配置 stationery.sms.provider=aliyun 并填好 AccessKey 后切到阿里云短信。
 */
public interface SmsProvider {

    String name();

    /** 是否模拟通道，模拟时前端可直接拿到验证码方便演示 */
    boolean mock();

    void sendCode(String phone, String code);
}
