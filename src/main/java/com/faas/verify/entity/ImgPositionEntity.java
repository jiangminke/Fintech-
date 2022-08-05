package com.faas.verify.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
@AllArgsConstructor
public class ImgPositionEntity {
    private String key;
    private double x;
    private double y;
}
