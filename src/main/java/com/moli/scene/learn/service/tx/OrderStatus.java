package com.moli.scene.learn.service.tx;


public enum OrderStatus {
    SUCCESS(0),
    CANCELED(1),
    TRYING(2);

    private Integer code;

    OrderStatus(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return this.code;
    }

}
