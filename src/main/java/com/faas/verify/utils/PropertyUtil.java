package com.faas.verify.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;


public final class PropertyUtil {

    public static final String SYSTEMPROPERTIES = "verifi.properties";
    public static final String BG_URL_ONCLICK_LIST = "bg.url.onclick.list";
    public static final String BG_URL_JIGSAW_LIST = "bg.url.jigsaw.list";
    public static final String DEFAULT_FONT = "default.font";
    public static final String BG_URL_PATH = "bg";
    public static final String FONT_URL_PATH = "font";
    public static final String BG_URL_SPLIT = ",";

    private static Logger logger = LoggerFactory.getLogger(PropertyUtil.class);
    private static Properties prop = new Properties();
    private static Map<String,List<BufferedImage>> cacheImg = null;// new ArrayList<>();
    private static Font defaultFont = null;

    private static Object objSyn = new Object();

    static {
        loadProp();
    }

    private static void loadProp() {
        try {
            prop.clear();
            prop.load(PropertyUtil.class.getClassLoader().getResourceAsStream(SYSTEMPROPERTIES));
        } catch (Exception e) {
            logger.error("load verifi.properties failed by : ", e);
        }
    }

    public static String getFontName(){
        return prop.getProperty(DEFAULT_FONT);
    }

    public static Font getFont(){
        if(defaultFont==null) {
            String fontName = prop.getProperty(DEFAULT_FONT);
            defaultFont = new Font(fontName, Font.BOLD, 30);
        }
        return defaultFont;
    }

    public static String getProperty(String key) {
        return prop.getProperty(key);
    }


    private static List<BufferedImage> getList(String urls){
        List<BufferedImage> list = new ArrayList<>();
        if(StringUtils.isNotEmpty(urls)) {
            for (String item : urls.split(BG_URL_SPLIT)) {
                java.net.URL imageURL = PropertyUtil.class.getClassLoader().getResource(BG_URL_PATH + "/" + item);
                //java.net.URL imageURL = ClassLoader.getSystemResource(BG_URL_PATH + "/" + item);
                try {
                    list.add(ImageIO.read(imageURL));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    private synchronized static void initImg(){
        if(cacheImg==null) {
            synchronized(objSyn) {
                if(cacheImg!=null){
                    return;
                }
                cacheImg = new HashMap<>();
                String urls = getProperty(BG_URL_ONCLICK_LIST);
                cacheImg.put(ConstUtil.CONS_POINT_CLICK_HANDLER, getList(urls));
                urls = getProperty(BG_URL_JIGSAW_LIST);
                cacheImg.put(ConstUtil.CONS_PUZZLE_HANDLER, getList(urls));
            }
        }
    }

    public static BufferedImage getSpecify(String handlerName,int k){
        initImg();
        List<BufferedImage> _cacheImg = cacheImg.get(handlerName);
        int index = k % _cacheImg.size();
        BufferedImage ret = _cacheImg.get(index);
        return copyImage(ret);
    }

    public static BufferedImage getRandom(String handlerName) {
        initImg();
        List<BufferedImage> _cacheImg = cacheImg.get(handlerName);
        int index = Double.valueOf(Math.random()*_cacheImg.size()).intValue();
        BufferedImage ret = _cacheImg.get(index);
        return copyImage(ret);
    }

    private static BufferedImage copyImage(BufferedImage source){
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }
}
