package ru.superplushkin.twofactorauthapp.subclasses;

import android.graphics.Color;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.model.SlidrPosition;

public class SlidrConfigHelper {
    public static SlidrConfig createRightEdgeConfig(SlidrListener listener) {
        return new SlidrConfig.Builder()
                .primaryColor(Color.BLACK)
                .secondaryColor(Color.BLACK)
                .position(SlidrPosition.LEFT)
                .sensitivity(1f)
                .scrimStartAlpha(0.8f)
                .scrimEndAlpha(0f)
                .velocityThreshold(2400)
                .distanceThreshold(0.25f)
                .edge(true)
                .edgeSize(0.3f)
                .listener(listener)
                .build();
    }
    public static SlidrConfig createLeftEdgeConfig(SlidrListener listener) {
        return new SlidrConfig.Builder()
                .primaryColor(Color.BLACK)
                .secondaryColor(Color.BLACK)
                .position(SlidrPosition.RIGHT)
                .sensitivity(1f)
                .scrimStartAlpha(0.8f)
                .scrimEndAlpha(0f)
                .velocityThreshold(2400)
                .distanceThreshold(0.25f)
                .edge(true)
                .edgeSize(0.3f)
                .listener(listener)
                .build();
    }
}