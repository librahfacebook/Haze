package com.example.haze.domain;

import org.litepal.crud.DataSupport;

/**
 * 定位城市的封装类
 */
public class Address extends DataSupport{
    private String address;
    private String cid;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }
}
