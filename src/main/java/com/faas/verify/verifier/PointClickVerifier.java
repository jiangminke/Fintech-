package com.faas.verify.verifier;

import com.alibaba.fastjson.JSON;
import com.faas.verify.utils.PropertyUtil;
import com.faas.verify.core.IVerifier;
import com.faas.verify.entity.CaptchaDataEntity;
import com.faas.verify.entity.ImgInteractionEntity;
import com.faas.verify.entity.ImgPositionEntity;
import com.faas.verify.entity.VerifyEntity;
import com.faas.verify.handler.bg.IBackgroundHandler;
import com.faas.verify.utils.ByteUtil;
import com.faas.verify.utils.ConstUtil;
import com.faas.verify.utils.DistanceUtil;
import com.faas.verify.utils.ImageUtil;
import lombok.Setter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


public class PointClickVerifier extends BaseVerifier implements IVerifier<VerifyEntity, CaptchaDataEntity<ImgInteractionEntity>> {
    private final static double PER = 75D;
    private final static double PER_OFFSET = 5D;
    private final static double SIM_THRESHOLD = 70D;
    private static final int BIGGER_CIRCLE_COUNT = 20;
    @Setter
    private List<String> keyNames;
    public PointClickVerifier(List<String> keyNames) {
        this.keyNames = keyNames;
    }

    @Override
    public boolean verifyCode(VerifyEntity verifyEntity) {
        List<ImgPositionEntity> data = JSON.parseArray(new String(verifyEntity.getOriginalByte()), ImgPositionEntity.class);
        List<ImgPositionEntity> chkPoints = JSON.parseArray(new String(verifyEntity.getVerifyByte()), ImgPositionEntity.class);

        Double[][] aa = new Double[data.size()][2];
        Double[][] bb = new Double[chkPoints.size()][2];

        if (aa.length != bb.length) {
            return false;
        }

        int index = 0;
        for (ImgPositionEntity pos : data) {
            aa[index][0] = pos.getX();
            aa[index][1] = pos.getY();
            index++;
        }

        index = 0;
        for (ImgPositionEntity pos : chkPoints) {
            bb[index][0] = pos.getX();
            bb[index][1] = pos.getY();
            index++;
        }

        double ret = DistanceUtil.simDistance(aa, bb);
        return ret < SIM_THRESHOLD;
    }

    @Override
    public CaptchaDataEntity<ImgInteractionEntity>[] buildCaptcha(Map<String, Object> buildParams) {
        List<ImgPositionEntity> ret = new LinkedList<>();
        char[] data = getKeyData().toCharArray();
        int count = data.length;
        int circleErr = 0;
        while (count > 0) {
            ImgPositionEntity _tmp = getRandomPostion(String.valueOf(data[data.length - count]),(Integer)(buildParams.get(ConstUtil.KEY_IMG_BG_WIDTH)),(Integer)(buildParams.get(ConstUtil.KEY_IMG_BG_HEIGHT)));
            boolean isInCir = false;
            for (ImgPositionEntity item : ret) {
                if (isInCircle(_tmp.getX(), _tmp.getY(), item.getX(), item.getY(), Double.valueOf (buildParams.get(ConstUtil.KEY_POINT_DIS_RADIUS).toString()))) {
                    isInCir = true;
                    break;
                }
            }
            if (isInCir) {
                if(circleErr> BIGGER_CIRCLE_COUNT){
                    throw new IllegalArgumentException("circle too bigger,pleace check KEY_POINT_DIS_RADIUS ");
                }
                continue;
            }
            circleErr = 0;
            ret.add(_tmp);
            count--;
        }
        buildParams.put(ConstUtil.IMG_CUT_SPLIE, ConstUtil.IMG_CUT_WIDTH);
        buildParams.put(ConstUtil.IMG_RANDOM_ARR, ByteUtil.toShortList(orderWidth((Integer)(buildParams.get(ConstUtil.KEY_IMG_BG_WIDTH)))));
        buildParams.put(ConstUtil.IMG_SIZE, 3);
        if(buildParams!=null && buildParams.containsKey(ConstUtil.IS_MIX_IMAGE)){
            buildParams.put(ConstUtil.IS_MIX_IMAGE,buildParams.get(ConstUtil.IS_MIX_IMAGE));
        }
        CaptchaDataEntity<ImgInteractionEntity>[] retCap = new CaptchaDataEntity[2];
        CaptchaDataEntity<ImgInteractionEntity> paimary = new CaptchaDataEntity<>();
        ImgInteractionEntity imgInteractionEntity = new ImgInteractionEntity();
        try {
            imgInteractionEntity.setImgByte(clickImage(ret, buildParams));

        } catch (Exception e) {

        }
        imgInteractionEntity.setImgPositionEntities(ret);
        paimary.setCaptchaType(CaptchaDataEntity.CaptchaType.PRIMARY_IMG);
        paimary.setOriginalObj(imgInteractionEntity);
        paimary.setParams(buildParams);
        retCap[0] = paimary;

        paimary = new CaptchaDataEntity<>();
        imgInteractionEntity = new ImgInteractionEntity();
        try {
            imgInteractionEntity.setImgByte(tipImage(ret, buildParams));

        } catch (Exception e) {

        }
        paimary.setCaptchaType(CaptchaDataEntity.CaptchaType.SECONDARY_IMG);
        paimary.setOriginalObj(imgInteractionEntity);
        paimary.setParams(buildParams);
        retCap[1] = paimary;
        return retCap;
    }

    private byte[] clickImage(List<ImgPositionEntity> postionLists, Map<String, Object> params) throws IOException {
        IBackgroundHandler backgroundHandler = (IBackgroundHandler)params.get(ConstUtil.CON_BG_HANDLER);
        BufferedImage d = backgroundHandler.getBackground((Integer)params.get(ConstUtil.KEY_IMG_BG_WIDTH),(Integer)params.get(ConstUtil.KEY_IMG_BG_HEIGHT));
        BufferedImage ret = null;
        for (ImgPositionEntity item : postionLists) {
            ret = setContent(d, item.getKey(), (float) item.getX(), (float) item.getY());
        }
        Object arr = params.get(ConstUtil.IMG_RANDOM_ARR);
        Object isMixImg = params.get(ConstUtil.IS_MIX_IMAGE);
        boolean boolIsMixImg = isMixImg != null ? (boolean) isMixImg : true;
        List<Integer> poxList = JSON.parseArray(JSON.toJSONString(arr), Integer.class);
        Integer weightSplit = Integer.valueOf(ConstUtil.IMG_CUT_WIDTH);
        if (boolIsMixImg) {
            ret = ImageUtil.imgRandom(ret, ByteUtil.toLongList(poxList), weightSplit);
        }
        ImageUtil.addBgPoint(ret);
        return ImageUtil.img2Byte(ret, IMG_TYPE);
    }

    private byte[] tipImage(List<ImgPositionEntity> postionLists, Map<String, Object> params) throws IOException {
        StringBuilder dataStr = new StringBuilder();
        dataStr.append("请依次点击:");
        dataStr.append(postionLists.stream().map(i -> i.getKey()).collect(Collectors.joining("")));
        int sizeFont = params.get(ConstUtil.CONS_POINT_CLICK_TIP_FONT_SIZE)!=null?Integer.parseInt(params.get(ConstUtil.CONS_POINT_CLICK_TIP_FONT_SIZE).toString()):28;
        return generateBufferedImage((Integer)params.get(ConstUtil.KEY_IMG_BG_WIDTH), sizeFont,dataStr.toString());
    }

    private String getKeyData() {
        return keyNames.get((int) (Math.random() * keyNames.size()));
    }

    private static double getRandomPoint() {
        return Math.random() * PER + PER_OFFSET;
    }

    private ImgPositionEntity getRandomPostion(String key, int gbWidth, int gbHeight) {
        Double xD = toScale(getRandomPoint());
        Double yD = toScale(getRandomPoint(20, 65));
        int x = Double.valueOf(gbWidth * (xD / SIM_PER)).intValue();
        int y = Double.valueOf(gbHeight * (yD / SIM_PER)).intValue();
        return new ImgPositionEntity(key, x, y);
    }

    /**
     * 判断一个点是否在圆形区域内
     *
     * @param pointLon 要判断的点的纵坐标
     * @param pointLat 要判断的点的横坐标
     * @param lon      圆心纵坐标
     * @param lat      圆心横坐标
     * @param radius   圆的半径
     * @return
     */
    private static boolean isInCircle(double pointLon, double pointLat, double lon, double lat,
                                      double radius) {
        double distance = Math.hypot((pointLon - lon), (pointLat - lat));
        if (distance > radius) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 修改图片,返回修改后的图片缓冲区（只输出一行文本）
     */
    private static BufferedImage setContent(BufferedImage img, String content, float xPer, float yPer) {

        try {
            int w = img.getWidth();
            int h = img.getHeight();
            Graphics2D g = img.createGraphics();
            g.setBackground(Color.WHITE);
            //Color c = getRandColor(220, 250);

//            Color c = new Color(255,(new Double(Math.random() * 128)).intValue() + 128,160);

            Color c = Color.WHITE;

            Font loadFont = PropertyUtil.getFont();

            loadFont.deriveFont(16);

            Random rand = new Random();
            g.setColor(c);//设置字体颜色
            g.setStroke(new BasicStroke(3));
            g.setFont(loadFont);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            if (content != null) {

                AffineTransform affine = new AffineTransform();
                affine.setToRotation(Math.PI / 4 * rand.nextDouble() * (rand.nextBoolean() ? 1 : -1),
                        xPer - 12, yPer + 10);
                g.setTransform(affine);
                g.drawString(content, xPer - 12, yPer + 10);
            }
            g.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return img;
    }

    private static byte[] generateBufferedImage(int w, int h,String code) throws IOException {
        int verifySize = code.length();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random rand = new Random();
        Graphics2D g2 = image.createGraphics();
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color c = ImageUtil.getRandColor(200, 250);
        g2.setColor(c);// 设置背景色
        g2.fillRect(0, 0, w, h);

        ImageUtil.addBgPoint(image);
//         shear(g2, w, h, c);// 使图片扭曲

        g2.setColor(ImageUtil.getRandColor(100, 160));
        int fontSize = h - 4;
        Font font = new Font(PropertyUtil.getFontName(), Font.ITALIC, fontSize);
        g2.setFont(font);
        char[] chars = code.toCharArray();
        for (int i = 0; i < verifySize; i++) {
            AffineTransform affine = new AffineTransform();
            affine.setToRotation(Math.PI / 4 * rand.nextDouble() * (rand.nextBoolean() ? 1 : -1),
                        (w / verifySize) * i + fontSize / 2, h / 2);
            g2.setTransform(affine);
            g2.drawChars(chars, i, 1, ((w - 10) / verifySize) * i + 5, h / 2 + fontSize / 2 - 5);
        }
        g2.dispose();
        return ImageUtil.img2Byte(image, IMG_TYPE);// 输出png图片
    }
}
