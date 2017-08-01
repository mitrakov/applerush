package ru.mitrakov.self.rush.stat;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.mitrakov.self.rush.PsObject;

/**
 * Created by mitrakov on 01.03.2017
 */
class ScreenStat extends ScreenAdapter {
    private static final String[] categories = new String[]{
            "Time elapsed:      ",
            "Current used SIDs: ",
            "Current battles:   ",
            "Current users:     ",
            "Total battles:     ",
            "Total users:       ",
            "Senders count:     ",
            "Receivers count:   ",
            "Current AI count:  ",
            "Total AI spawned:  ",
            "Battle refs up:    ",
            "Battle refs down:  ",
            "Round refs up:     ",
            "Round refs down:   ",
            "Field refs up:     ",
            "Field refs down:   ",
            "Current env size:  ",
    };

    private final PsObject psObject;
    private final Skin skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
    private final Stage stage = new Stage(new FitViewport(Stat.WIDTH, Stat.HEIGHT));
    private final Table tableLeft = new Table();
    private final Table tableRight = new Table();
    private final Label lblStatus = new Label("", skin, "white");
    private final IntMap<Label> lblValues = new IntMap<Label>(16);

    private transient int status = 0;

    ScreenStat(PsObject psObject) {
        this.psObject = psObject;

        Table table = new Table();
        table.setFillParent(true);
        table.add(tableLeft).space(20);
        table.add(tableRight).space(20);
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK))
            psObject.hide();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    ScreenStat init() {
        for (int i = 0; i < categories.length; i++) {
            Label category = new Label(categories[i], skin, "white");
            Label value = new Label("0", skin, "white");
            lblValues.put(i, value);
            tableLeft.add(category).left();
            tableLeft.add(value).right();
            tableLeft.row();
        }
        tableRight.add(lblStatus).width(50);
        return this;
    }

    void onReceived() {
        status = (status + 1) % 4;
        lblStatus.setText(status == 1 ? "." : status == 2 ? ".." : status == 3 ? "..." : "");
    }

    void setValue(int category, int value) {
        Label label = lblValues.get(category);
        if (label != null)
            label.setText(String.valueOf(value));
    }
}
