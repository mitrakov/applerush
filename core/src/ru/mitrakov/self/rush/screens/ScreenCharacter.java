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
import ru.mitrakov.self.rush.dialogs.DialogFood;

/**
 * Created by mitrakov on 01.03.2017
 */
public class ScreenCharacter extends LocalizableScreen {

    private final Array<TextButton> checkboxes = new Array<TextButton>(4);
    private final TextButton btnNext;
    private final DialogFood dialog;

    public ScreenCharacter(Winesaps game, final Model model, PsObject psObject, AssetManager assetManager,
                           AudioManager audioManager) {
        super(game, model, psObject, assetManager, audioManager);
        Skin skin = assetManager.get("skin/uiskin.json");

        Array<Actor> images = init(skin);
        btnNext = createButton(skin);
        dialog = createFoodDialog(skin);
        buildTable(images, skin);
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
        dialog.onLocaleChanged(bundle);
        btnNext.setText(bundle.format("next"));
    }

    @Override
    public void handleEvent(EventBus.Event event) {
    }

    @Override
    public void handleEventBackground(EventBus.Event event) {
    }

    private Array<Actor> init(Skin skin) {
        Array<Actor> result = new Array<Actor>(model.characterValues.length);

        TextureAtlas atlasCharacter = assetManager.get("pack/char.pack");
        for (Model.Character character : model.characterValues) {
            if (character != Model.Character.None) {
                // create checkboxes
                final TextButton btn = new CheckBox("", skin, "default");
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

    private TextButton createButton(Skin skin) {
        return new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    for (TextButton btn : checkboxes) {
                        if (btn.isChecked()) {
                            Object obj = btn.getUserObject();
                            if (obj instanceof Model.Character) { // stackoverflow.com/questions/2950319
                                model.changeCharacter((Model.Character) obj);
                                dialog.setCharacter((Model.Character) obj).show(stage);
                            }
                        }
                    }
                }
            });
        }};
    }

    private DialogFood createFoodDialog(Skin skin) {
        DialogFood dialog = new DialogFood("", skin, "default", assetManager.<TextureAtlas>get("pack/menu.pack"));
        dialog.setOnResultAction(new Runnable() {
            @Override
            public void run() {
                game.setNextScreen();
            }
        });
        return dialog;
    }

    private void buildTable(Array<Actor> images, Skin skin) {
        Table tableMain = new Table(skin);
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
