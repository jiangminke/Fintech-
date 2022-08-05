package com.faas.verify.utils;


public class DistanceUtil {
    public static double simDistance(Double[][] vector1, Double[][] vector2) {
        double distance = 0;
        if (vector1.length == vector2.length) {
            for (int i = 0; i < vector1.length; i++) {
                double temp = Math.pow((vector1[i][0] - vector2[i][0]), 2);
                double temp2 = Math.pow((vector1[i][1] - vector2[i][1]), 2);
                distance += temp + temp2;
            }
            distance = Math.sqrt(distance);
        }
        return distance;
    }
}
