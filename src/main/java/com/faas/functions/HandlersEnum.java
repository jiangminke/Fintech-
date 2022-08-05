package com.faas.functions;

import com.faas.verify.handler.ImgHttpBaseHandler;
import com.faas.verify.handler.PointClickHander;
import com.faas.verify.handler.PuzzleHandler;
import com.faas.verify.utils.ConstUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

public enum HandlersEnum {
    CLICK(ConstUtil.CONS_POINT_CLICK_HANDLER, PointClickHander.INSTANCE),
    PUZZLE(ConstUtil.CONS_PUZZLE_HANDLER, PuzzleHandler.INSTANCE),
    ;


    HandlersEnum(String name, ImgHttpBaseHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private ImgHttpBaseHandler handler;

    public static ImgHttpBaseHandler getHandler(String handlerName) {
        if (StringUtils.isBlank(handlerName)) {
            return null;
        }

        for (HandlersEnum handlersEnum : HandlersEnum.values()) {
            if (handlersEnum.getName().equals(handlerName)) {
                return handlersEnum.getHandler();
            }
        }
        return null;
    }

}
