package ru.mitrakov.self.rush;

import android.os.*;
import android.view.View;
import android.graphics.Rect;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.*;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useCompass = false;
        config.useAccelerometer = false;
        config.useGyroscope = false;

        final PsObject obj = new AndroidPsObject(this);

        if (Build.VERSION.SDK_INT >= 11) { // "addOnLayoutChangeListener" requires Level API 11
            getWindow().getDecorView().getRootView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bot, int a, int b, int c, int d) {
                    Rect rect = new Rect();
                    v.getWindowVisibleDisplayFrame(rect);
                    obj.raiseRatioChanged(1f * rect.width() / rect.height());
                }
            });
        }

        initialize(new Winesaps(obj), config);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Gdx.graphics.requestRendering(); // BUG in LibGDX! (http://badlogicgames.com/forum/viewtopic.php?f=11&t=17257)
    }
}
