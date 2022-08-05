package com.faas.verify.entity;


import lombok.Data;

import java.util.Date;
@Data
public class UserRecord {
    private Date lastForbidTime = new Date();
    private int state = 0;
    private int failTimes = 0;
}
