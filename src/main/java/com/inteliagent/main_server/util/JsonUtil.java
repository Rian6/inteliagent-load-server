package com.inteliagent.main_server.util;

import com.google.gson.Gson;

public class JsonUtil {

    private static final Gson gson = new Gson();

    public static <T> String toJson(T obj) {
        return gson.toJson(obj);
    }
}
