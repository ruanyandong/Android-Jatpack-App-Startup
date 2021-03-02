package com.ryd.appstartup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import org.litepal.LitePal;

import java.util.Collections;
import java.util.List;

/**
 * java写法
 */
public class LitaPalInitializer implements Initializer<Void> {

    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        LitePal.initialize(context);
        return null;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
