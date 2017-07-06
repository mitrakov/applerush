package ru.mitrakov.self.rush.screens;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;

import ru.mitrakov.self.rush.*;
import ru.mitrakov.self.rush.ui.*;
import ru.mitrakov.self.rush.model.*;
import ru.mitrakov.self.rush.dialogs.*;
import ru.mitrakov.self.rush.model.Cells.*;

/**
 * Created by mitrakov on 01.03.2017
 */
public class ScreenTraining extends LocalizableScreen {

    private final Gui gui;
    private final ImageButton btnThing;
    private final TextButton btnSkip;
    private final DialogFinished finishedDialog;
    private final DialogTraining trainingDialog;

    private final ObjectMap<Class, Drawable> things = new ObjectMap<Class, Drawable>(2);
    private final Queue<Window> curtains = new Queue<Window>(3);

    public ScreenTraining(final Winesaps game, final Model model, PsObject psObject, AssetManager assetManager,
                          Skin skin, AudioManager manager) {
        super(game, model, psObject, assetManager, skin, manager);

        loadTextures();
        gui = new Gui(model, assetManager);
        finishedDialog = new DialogFinished(skin, "default");
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
    public void show() {
        super.show();
        if (model.newbie)
            model.receiveTraining();
        else game.setNextScreen();
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
        if (event instanceof EventBus.RoundStartedEvent) {
            EventBus.RoundStartedEvent ev = (EventBus.RoundStartedEvent) event;
            if (ev.number == 0)
                trainingDialog.show(stage);
        }
        if (event instanceof EventBus.RoundFinishedEvent) {
            if (model.newbie) { // check is necessary because RoundFinishedEvent is raised once again after giveUp()
                model.newbie = false;
                model.giveUp();
                trainingDialog.remove();
                finishedDialog.setScore(1, 0).setOnResultAction(new Runnable() {
                    @Override
                    public void run() {
                        game.setNextScreen();
                    }
                }).show(stage);
            }
        }
        if (event instanceof EventBus.GameFinishedEvent) {
            gui.setMovesAllowed(false); // forbid moving to restrict sending useless messages to the server
        }
        if (event instanceof EventBus.ScoreChangedEvent) {
            EventBus.ScoreChangedEvent ev = (EventBus.ScoreChangedEvent) event;
            if (ev.score1 > 0 && curtains.size > 0)
                curtains.removeFirst().remove();
            trainingDialog.next();
        }
        if (event instanceof EventBus.ThingChangedEvent) {
            EventBus.ThingChangedEvent ev = (EventBus.ThingChangedEvent) event;
            if (ev.mine) {
                // 1) change button image
                Class clazz = ev.newThing != null ? ev.newThing.getClass() : CellObject.class;
                ImageButton.ImageButtonStyle style = btnThing.getStyle();
                if (style != null)
                    style.imageUp = things.get(clazz);
                // 2) show the next dialog tip
                if (ev.oldThing != null || ev.newThing != null)
                    trainingDialog.next();
                // 3) forbid moving to make a user use the umbrella (note#1)
                gui.setMovesAllowed(ev.newThing == null);
            }
        }
    }

    @Override
    public void handleEventBackground(EventBus.Event event) {
    }

    private void loadTextures() {
        TextureAtlas atlasThing = assetManager.get("pack/thing.pack");
        for (Class clazz : new Class[]{CellObject.class, UmbrellaThing.class}) {
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
        TextureAtlas atlas = assetManager.get("pack/training.pack");
        trainingDialog.clearMessages()
                .addMessage(atlas.findRegion("msg1"), i18n.format("train.msg1.text"), i18n.format("train.msg1.action"))
                .addMessage(atlas.findRegion("msg2"), i18n.format("train.msg2.text"), i18n.format("train.msg2.action"))
                .addMessage(atlas.findRegion("msg3"), i18n.format("train.msg3.text"), i18n.format("train.msg3.action"))
                .addMessage(atlas.findRegion("msg4"), i18n.format("train.msg4.text"), i18n.format("train.msg4.action"))
                .addMessage(atlas.findRegion("msg5"), i18n.format("train.msg5.text"), i18n.format("train.msg5.action"))
                .addMessage(atlas.findRegion("msg6"), i18n.format("train.msg6.text"), i18n.format("train.msg6.action"));
    }
}

// note#1 (@mitrakov, 2017-03-24): even though bool condition "thing==null" is unreliable, an actor won't die because
// waterfall is fake
