package com.nebula.stationery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.nebula.stationery.mapper")
@EnableScheduling
public class StationeryApplication {

    public static void main(String[] args) {
        SpringApplication.run(StationeryApplication.class, args);
    }
}
