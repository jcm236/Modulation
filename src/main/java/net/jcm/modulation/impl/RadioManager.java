package net.jcm.modulation.impl;

import net.jcm.modulation.api.IRadioFieldManager;

public class RadioManager {
    private static IRadioFieldManager INSTANCE;

    public static void init(IRadioFieldManager impl) {
        INSTANCE = impl;
        INSTANCE.start();
    }

    public static void shutdown() {
        INSTANCE.shutdown();
        INSTANCE = null;
    }

    public static IRadioFieldManager getInstance() {
        return INSTANCE;
    }
}
