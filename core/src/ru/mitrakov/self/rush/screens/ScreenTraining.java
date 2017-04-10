package ru.mitrakov.self.rush.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ru.mitrakov.self.rush.*;
import ru.mitrakov.self.rush.ui.*;
import ru.mitrakov.self.rush.dialogs.*;
import ru.mitrakov.self.rush.model.Model;
import ru.mitrakov.self.rush.model.object.*;

/**
 * Created by mitrakov on 01.03.2017
 */

public class ScreenTraining extends LocalizableScreen {
    private final Model model;
    private final RushClient game;
    private final PsObject psObject;
    private final Stage stage = new Stage(new FitViewport(RushClient.WIDTH, RushClient.HEIGHT));
    private final TextureAtlas atlasTraining = new TextureAtlas(Gdx.files.internal("pack/training.pack"));
    private final TextureAtlas atlasThing = new TextureAtlas(Gdx.files.internal("pack/thing.pack"));

    private final Table table = new Table();
    private final Gui gui;
    private final ImageButton btnThing;
    private final TextButton btnSkip;
    private final DialogFinished finishedDialog;
    private final DialogTraining trainingDialog;

    private final ObjectMap<Class, Drawable> things = new ObjectMap<Class, Drawable>(2);
    private final Queue<Window> curtains = new Queue<Window>(3);

    private int score = 0;
    private CellObject thing = null;
    private boolean started = false;
    private boolean finished = false;
    private I18NBundle i18n;

    public ScreenTraining(final RushClient game, final Model model, PsObject psObject, Skin skin,
                          AudioManager audioManager, I18NBundle i18n) {
        assert game != null && model != null && skin != null && i18n != null; // psObject, audioManager may be NULL
        this.model = model;
        this.game = game;
        this.psObject = psObject; // may be NULL
        this.i18n = i18n;

        table.setFillParent(true);
        stage.addActor(table);

        loadTextures();
        gui = new Gui(model);
        finishedDialog = new DialogFinished(game, skin, "default", i18n);
        trainingDialog = new DialogTraining(skin, "default");
        btnThing = new ImageButtonFeat(things.get(CellObject.class), audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.useThing();
                }
            });
        }};
        btnSkip = new TextButtonFeat(i18n.format("train.skip"), skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.newbie = false;
                    model.stopBattle();
                    game.setNextScreen();
                }
            });
        }};

        initComponents(skin);
        addContent(i18n);
    }

    @Override
    public void render(float delta) {
        // redraw all
        Gdx.gl.glClearColor(.35f, .87f, .91f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();

        // updating the thing
        Class clazz = model.curThing != null ? model.curThing.getClass() : CellObject.class;
        btnThing.getStyle().imageUp = things.get(clazz); // here getStyle() != NULL

        // checking
        checkStarted();
        checkNextMessage();
        checkFinished();

        // checking BACK and MENU buttons on Android
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK))
            if (psObject != null)
                psObject.hide();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        if (model.newbie) {
            model.receiveTraining();
            Gdx.input.setInputProcessor(stage);
        } else game.setNextScreen();
    }

    @Override
    public void dispose() {
        stage.dispose();
        atlasTraining.dispose(); // disposing an atlas also disposes all its internal textures
        atlasThing.dispose();
        gui.dispose();
        super.dispose();
    }

    @Override
    public void onLocaleChanged(I18NBundle bundle) {
        assert bundle != null;
        this.i18n = bundle;

        btnSkip.setText(bundle.format("train.skip"));
        finishedDialog.onLocaleChanged(bundle);
        addContent(bundle);
    }

    private void loadTextures() {
        for (Class clazz : new Class[]{CellObject.class, Umbrella.class}) {
            TextureRegion region = atlasThing.findRegion(clazz.getSimpleName());
            if (region != null)
                things.put(clazz, new TextureRegionDrawable(region));
        }
    }

    private void initComponents(Skin skin) {
        // building table
        table.add(gui).colspan(2);
        table.row();
        table.add(btnThing).align(Align.left);
        table.add(btnSkip).align(Align.right).width(200).height(btnThing.getHeight());

        // initialize curtains windows
        Window window;
        window = new Window("", skin, "default");
        window.setBounds(235, 380, 125, 268);
        curtains.addLast(window);
        window = new Window("", skin, "default");
        window.setBounds(135, 212, 105, 180);
        curtains.addLast(window);
        window = new Window("", skin, "default");
        window.setBounds(35, 212, 105, 180);
        curtains.addLast(window);
        for (Window w : curtains) {
            w.setTouchable(Touchable.disabled); // skip touch events through the window
            stage.addActor(w);
        }
    }

    private void addContent(I18NBundle i18n) {
        // note: if atlas.findRegion() returns null, the image would be empty (no Exceptions expected)
        final TextureAtlas atlas = atlasTraining;
        trainingDialog.clearMessages()
                .addMessage(atlas.findRegion("msg1"), i18n.format("train.msg1.text"), i18n.format("train.msg1.action"))
                .addMessage(atlas.findRegion("msg2"), i18n.format("train.msg2.text"), i18n.format("train.msg2.action"))
                .addMessage(atlas.findRegion("msg3"), i18n.format("train.msg3.text"), i18n.format("train.msg3.action"))
                .addMessage(atlas.findRegion("msg4"), i18n.format("train.msg4.text"), i18n.format("train.msg4.action"))
                .addMessage(atlas.findRegion("msg5"), i18n.format("train.msg5.text"), i18n.format("train.msg5.action"))
                .addMessage(atlas.findRegion("msg6"), i18n.format("train.msg6.text"), i18n.format("train.msg6.action"));
    }

    private void checkStarted() {
        if (!started && model.field != null) {
            started = true;
            trainingDialog.show(stage).next();
        }
    }

    private void checkNextMessage() {
        if (score != model.score1) {
            score = model.score1;
            trainingDialog.next();
            if (curtains.size > 0)
                curtains.removeFirst().remove();
        }
        if (thing != model.curThing) {
            thing = model.curThing;
            trainingDialog.next();
            gui.setMovesAllowed(thing == null); // forbid moving to make a user use the umbrella (see note#1)
        }
    }

    private void checkFinished() {
        assert i18n != null;
        if (!finished && model.roundFinishedTime > 0) {
            finished = true;
            model.newbie = false;
            model.stopBattle();
            trainingDialog.remove();
            finishedDialog.setText(i18n.format("train.msgX.text"), i18n.format("train.msgX.action")).setScore(1, 0)
                    .setQuitOnResult(true).show(stage);
        }
    }
}

// note#1 (@mitrakov, 2017-03-24): even though bool condition "thing==null" is unreliable, an actor won't die because
// waterfall is fake
