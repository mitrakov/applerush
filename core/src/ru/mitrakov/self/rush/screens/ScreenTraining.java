package ru.mitrakov.self.rush.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;

import ru.mitrakov.self.rush.*;
import ru.mitrakov.self.rush.ui.*;
import ru.mitrakov.self.rush.model.*;
import ru.mitrakov.self.rush.dialogs.*;
import ru.mitrakov.self.rush.model.object.*;

/**
 * Created by mitrakov on 01.03.2017
 */
public class ScreenTraining extends LocalizableScreen {

    private final TextureAtlas atlasTraining = new TextureAtlas(Gdx.files.internal("pack/training.pack"));
    private final TextureAtlas atlasThing = new TextureAtlas(Gdx.files.internal("pack/thing.pack"));
    private final Gui gui;
    private final ImageButton btnThing;
    private final TextButton btnSkip;
    private final DialogFinished finishedDialog;
    private final DialogTraining trainingDialog;

    private final ObjectMap<Class, Drawable> things = new ObjectMap<Class, Drawable>(2);
    private final Queue<Window> curtains = new Queue<Window>(3);

    private CellObject thing = null;
    private boolean started = false;

    public ScreenTraining(final RushClient game, final Model model, PsObject psObject, Skin skin, AudioManager manager) {
        super(game, model, psObject, skin, manager);

        loadTextures();
        gui = new Gui(model);
        finishedDialog = new DialogFinished(game, skin, "default");
        trainingDialog = new DialogTraining(skin, "panel-maroon");
        btnThing = new ImageButtonFeat(things.get(CellObject.class), audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.useThing();
                }
            });
        }};
        btnSkip = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.newbie = false;
                    model.giveUp();
                    game.setNextScreen();
                }
            });
        }};

        initComponents();
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // checking
        checkStarted();
    }

    @Override
    public void show() {
        super.show();
        if (model.newbie)
            model.receiveTraining();
        else game.setNextScreen();
    }

    @Override
    public void dispose() {
        atlasTraining.dispose(); // disposing an atlas also disposes all its internal textures
        atlasThing.dispose();
        gui.dispose();
        super.dispose();
    }

    @Override
    public void onLocaleChanged(I18NBundle bundle) {
        super.onLocaleChanged(bundle);
        assert bundle != null;

        btnSkip.setText(bundle.format("train.skip"));
        finishedDialog.setText(bundle.format("train.msgX.text"), bundle.format("train.msgX.action"));
        finishedDialog.onLocaleChanged(bundle);
        addContent(bundle);
    }

    @Override
    public void handleEvent(EventBus.Event event) {
        if (model.newbie) { // to avoid collisions with BattleScreen
            if (event instanceof EventBus.RoundFinishedEvent) {
                model.newbie = false;
                gui.setMovesAllowed(false); // forbid moving to restrict sending useless messages to the server
                model.giveUp();
                trainingDialog.remove();
                finishedDialog.setScore(1, 0).setQuitOnResult(true).show(stage);
            }
            if (event instanceof EventBus.ScoreChangedEvent) {
                EventBus.ScoreChangedEvent ev = (EventBus.ScoreChangedEvent) event;
                if (ev.score1 + ev.score2 > 0) {
                    trainingDialog.next();
                    if (curtains.size > 0)
                        curtains.removeFirst().remove();
                }
            }
            if (event instanceof EventBus.ThingChangedEvent) {
                EventBus.ThingChangedEvent ev = (EventBus.ThingChangedEvent) event;
                // 1) change button image
                if (ev.mine) {
                    Class clazz = ev.newThing != null ? ev.newThing.getClass() : CellObject.class;
                    ImageButton.ImageButtonStyle style = btnThing.getStyle();
                    if (style != null)
                        style.imageUp = things.get(clazz);
                }
                // 2) show the next dialog tip
                trainingDialog.next();
                gui.setMovesAllowed(thing == null); // forbid moving to make a user use the umbrella (see note#1)
            }
        }
    }

    private void loadTextures() {
        for (Class clazz : new Class[]{CellObject.class, Umbrella.class}) {
            TextureRegion region = atlasThing.findRegion(clazz.getSimpleName());
            if (region != null)
                things.put(clazz, new TextureRegionDrawable(region));
        }
    }

    private void initComponents() {
        // building table
        table.add(gui).colspan(2);
        table.row();
        table.add(btnThing).align(Align.left).padLeft(2);
        table.add(btnSkip).align(Align.right).padRight(2).width(200).height(btnThing.getHeight());

        // initialize curtains windows
        Window window;
        window = new Window("", skin, "panel-black");
        window.setBounds(235, 380, 125, 268);
        curtains.addLast(window);
        window = new Window("", skin, "panel-black");
        window.setBounds(135, 212, 105, 180);
        curtains.addLast(window);
        window = new Window("", skin, "panel-black");
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
}

// note#1 (@mitrakov, 2017-03-24): even though bool condition "thing==null" is unreliable, an actor won't die because
// waterfall is fake
