package com.faas.verify.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class ImgInteractionEntity implements Serializable {
    private List<ImgPositionEntity> imgPositionEntities;
    private byte[] imgByte;
}
