package ru.mitrakov.self.rush.ui;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Created by mitrakov on 14.06.2017
 */

class AnimationData<T> {
    enum AnimationType {Idle, Run, Climb, Enter, Leave}

    private static class AnimChar {
        private final ObjectMap<AnimationType, Animation<TextureRegion>> animations =
                new ObjectMap<AnimationType, Animation<TextureRegion>>(AnimationType.values().length);

        AnimChar(TextureAtlas atlas, float frameDuration) {
            for (AnimationType type : AnimationType.values()) {
                Animation.PlayMode mode = type == AnimationType.Enter || type == AnimationType.Leave
                        ? Animation.PlayMode.NORMAL
                        : Animation.PlayMode.LOOP;
                float duration = type != AnimationType.Climb ? frameDuration : .03f;
                Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(type.name());
                Animation<TextureRegion> animation = new Animation<TextureRegion>(duration, frames, mode);
                animations.put(type, animation);
            }
        }
    }

    final float speedX;
    float x, y;
    boolean dirRight = true;

    private final ObjectMap<T, AnimChar> animChars = new ObjectMap<T, AnimChar>();
    private AnimationType curType = AnimationType.Idle;
    private float t;

    AnimationData(float speedX) {
        this.speedX = speedX;
    }

    public AnimationData add(T key, TextureAtlas atlas, float frameDuration) {
        animChars.put(key, new AnimChar(atlas, frameDuration));
        return this;
    }

    TextureRegion getFrame(T key) {
        AnimChar animChar = animChars.get(key);
        if (animChar != null) {
            Animation<TextureRegion> animation = animChar.animations.get(curType);
            if (animation != null)
                return animation.getKeyFrame(t);
        }
        return null;
    }

    int getWidth(T key) {
        AnimChar animChar = animChars.get(key);
        if (animChar != null) {
            Animation<TextureRegion> animation = animChar.animations.get(AnimationType.Run); // "Run" is usually wider
            if (animation != null)
                return animation.getKeyFrame(t).getRegionWidth();
        }
        return 0;
    }

    void addDt(float dt) {
        t += dt;
    }

    void setAnimation(AnimationType type, boolean value) {
        switch (type) {
            case Run:
            case Climb:
                if (value && curType != type) {
                    curType = type;
                    t = 0;
                }
                if (!value && curType == type) {
                    curType = AnimationType.Idle;
                    t = 0;
                }
                break;
        }
    }
}
