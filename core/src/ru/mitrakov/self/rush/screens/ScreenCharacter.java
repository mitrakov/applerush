package ru.mitrakov.self.rush.screens;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import ru.mitrakov.self.rush.*;
import ru.mitrakov.self.rush.ui.*;
import ru.mitrakov.self.rush.model.*;

/**
 * Created by mitrakov on 01.03.2017
 */
public class ScreenCharacter extends LocalizableScreen {

    private final Array<TextButton> checkboxes = new Array<TextButton>(4);
    private final TextButton btnNext;

    public ScreenCharacter(Winesaps game, final Model model, PsObject psObject, AssetManager assetManager,
                           AudioManager audioManager) {
        super(game, model, psObject, assetManager, audioManager);

        Array<Actor> images = init();
        btnNext = createButton();
        buildTable(images);
    }

    @Override
    public void show() {
        super.show();
        if (!model.newbie)
            game.setNextScreen();
    }

    @Override
    public void onLocaleChanged(I18NBundle bundle) {
        super.onLocaleChanged(bundle);
        assert bundle != null;

        for (TextButton btn : checkboxes) {
            Object obj = btn.getUserObject();
            if (obj instanceof Model.Character) {  // stackoverflow.com/questions/2950319
                btn.setText(bundle.format("character." + obj));
            }
        }
        btnNext.setText(bundle.format("next"));
    }

    @Override
    public void handleEvent(EventBus.Event event) {
    }

    @Override
    public void handleEventBackground(EventBus.Event event) {
    }

    private Array<Actor> init() {
        Array<Actor> result = new Array<Actor>(Model.Character.values().length);

        TextureAtlas atlasCharacter = assetManager.get("pack/char.pack");
        for (Model.Character character : Model.Character.values()) {
            if (character != Model.Character.None) {
                // create checkboxes
                final TextButton btn = new CheckBox("", assetManager.<Skin>get("skin/uiskin.json"), "default");
                btn.setUserObject(character);
                checkboxes.add(btn);

                // create pictures
                TextureRegion region = atlasCharacter.findRegion(character.name());
                if (region != null) {
                    result.add(new ImageButtonFeat(new TextureRegionDrawable(region), audioManager) {{
                        addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                btn.setChecked(!btn.isChecked());
                            }
                        });
                    }});
                }
            }
        }

        if (checkboxes.size > 0) {
            checkboxes.first().setChecked(true);
            new ButtonGroup<Button>((Button[]) (checkboxes.toArray(Button.class)));
        }

        return result;
    }

    private TextButton createButton() {
        return new TextButtonFeat("", assetManager.<Skin>get("skin/uiskin.json"), "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    for (TextButton btn : checkboxes) {
                        if (btn.isChecked()) {
                            Object obj = btn.getUserObject();
                            if (obj instanceof Model.Character) { // stackoverflow.com/questions/2950319
                                model.changeCharacter((Model.Character) obj);
                                game.setNextScreen();
                            }
                        }
                    }
                }
            });
        }};
    }

    private void buildTable(Array<Actor> images) {
        Table tableMain = new Table(assetManager.<Skin>get("skin/uiskin.json"));
        tableMain.pad(20).setBackground("panel-maroon");

        table.add(tableMain).expand();
        table.row().pad(5);
        table.add(btnNext).colspan(checkboxes.size).width(200).height(50).right();
        table.setBackground(new Image(assetManager.<Texture>get("back/main.jpg")).getDrawable());

        for (Actor img : images) {
            tableMain.add(img).bottom().space(20);
        }
        tableMain.row();
        for (TextButton btn : checkboxes) {
            tableMain.add(btn).space(20);
        }
    }
}
