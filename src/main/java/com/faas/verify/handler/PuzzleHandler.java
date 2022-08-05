package com.faas.verify.handler;

import com.faas.verify.verifier.PuzzleVerifier;
import com.faas.verify.utils.ConstUtil;

import java.util.Map;


public class PuzzleHandler extends ImgHttpBaseHandler {
    public static final PuzzleHandler INSTANCE = new PuzzleHandler();

    public PuzzleHandler() {
        this.setCaptchaVerifier(new PuzzleVerifier());
    }


    @Override
    public String getHandlerName() {
        return ConstUtil.CONS_PUZZLE_HANDLER;
    }


    @Override
    public void initBuildParams() {
        if(!childParams.containsKey(ConstUtil.KEY_IMG_PUZZLE_RADIUS)){
            childParams.put(ConstUtil.KEY_IMG_PUZZLE_RADIUS,ConstUtil.DEF_IMG_PUZZLE_RADIUS);
        }

        if(!childParams.containsKey(ConstUtil.KEY_IMG_PUZZLE_SPLIT_LINE)){
            childParams.put(ConstUtil.KEY_IMG_PUZZLE_SPLIT_LINE,ConstUtil.DEF_IMG_PUZZLE_SPLIT_LINE);
        }
    }

    @Override
    protected void afterBuildParams(Map<String, Object> bizParams) {
        super.afterBuildParams(bizParams);

        int jigsawWidthHeight;
        float radius = 5f;
        float splitLine = 15f;
        if(bizParams.containsKey(ConstUtil.KEY_IMG_PUZZLE_RADIUS)) {
            radius = (Float) bizParams.get(ConstUtil.KEY_IMG_PUZZLE_RADIUS);
            bizParams.remove(ConstUtil.KEY_IMG_PUZZLE_RADIUS);
        }

        if(bizParams.containsKey(ConstUtil.KEY_IMG_PUZZLE_SPLIT_LINE)) {
            splitLine = (Float) bizParams.get(ConstUtil.KEY_IMG_PUZZLE_SPLIT_LINE);
            bizParams.remove(ConstUtil.KEY_IMG_PUZZLE_SPLIT_LINE);
        }
        jigsawWidthHeight = Float.valueOf(radius*3+splitLine*2).intValue();
        bizParams.put(ConstUtil.KEY_IMG_JIGSAW_WIDTH,jigsawWidthHeight);
        bizParams.put(ConstUtil.KEY_IMG_JIGSAW_HEIGHT,jigsawWidthHeight);
    }
}
