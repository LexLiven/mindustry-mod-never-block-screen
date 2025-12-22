package qolneverlockscreen;

//import javax.swing.event.DocumentEvent.EventType;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import arc.ApplicationListener;

public class QoLNeverLockScreenMod extends Mod {

    private static final String PLAY_KEY  = "qol-nolock-play";
    private static final String PAUSE_KEY = "qol-nolock-pause";

    private boolean settingsAdded = false;

    @Override
    public void init() {
        // Ждём, пока UI гарантированно инициализируется
        Events.run(Trigger.update, () -> {
            if (!settingsAdded && Vars.ui != null && Vars.ui.settings != null) {
                addSettings();
                settingsAdded = true;
            }
        });

        // применяем логику каждый тик (дёшево и надёжно)
        Events.run(Trigger.update, this::apply);
    }

    private void apply() {
        // если игра не запущена — ничего не делаем
        if (!Vars.state.isGame()) {
            setKeepScreenOn(false);
            return;
        }

        boolean paused = Vars.state.isPaused();

        boolean keepOn = paused
                ? Core.settings.getBool(PAUSE_KEY, false)
                : Core.settings.getBool(PLAY_KEY, true);

        setKeepScreenOn(keepOn);
    }

    private void addSettings() {
        SettingsMenuDialog.SettingsTable graphics = Vars.ui.settings.graphics;

        graphics.checkPref(
        	    Core.bundle.get("setting.nosleep.play"),
        	    Core.settings.getBool(PLAY_KEY, true),
        	    v -> Core.settings.put(PLAY_KEY, v)
        	);

        	graphics.checkPref(
        	    Core.bundle.get("setting.nosleep.pause"),
        	    Core.settings.getBool(PAUSE_KEY, false),
        	    v -> Core.settings.put(PAUSE_KEY, v)
        	);
    }

    // ⚠ Единственный реально рабочий способ для модов
    private static void setKeepScreenOn(boolean enabled) {
        try {
            // Общий вариант для Desktop
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object app = gdx.getField("app").get(null);

            // Desktop
            try {
                app.getClass()
                   .getMethod("setIdleTimerDisabled", boolean.class)
                   .invoke(app, enabled);
            } catch (NoSuchMethodException ignored) {}

            // Android
            try {
                Class<?> androidApp = Class.forName("com.badlogic.gdx.backends.android.AndroidApplication");
                if (androidApp.isInstance(app)) {
                    Object window = androidApp.getMethod("getWindow").invoke(app);
                    Class<?> windowClass = Class.forName("android.view.Window");
                    int flag = Class.forName("android.view.WindowManager$LayoutParams")
                                   .getField("FLAG_KEEP_SCREEN_ON").getInt(null);
                    if (enabled) {
                        windowClass.getMethod("addFlags", int.class).invoke(window, flag);
                    } else {
                        windowClass.getMethod("clearFlags", int.class).invoke(window, flag);
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {
            // если совсем не удалось — молча
        }
    }
}