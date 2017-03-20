package ru.mitrakov.self.rush.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import ru.mitrakov.self.rush.PsObject;
import ru.mitrakov.self.rush.RushClient;
import ru.mitrakov.self.rush.model.Model;

/**
 * Created by mitrakov on 01.03.2017
 */

public class ScreenLogin extends ScreenAdapter {
    private final RushClient game;
    private final Model model;
    private final PsObject psObject;
    private final Stage stage = new Stage(new FitViewport(800, 480));
    private final Table table = new Table();

    private final TextField txtLogin; // ....
    private final TextField txtPassword;
    private final TextField txtEmail;
    private final TextField txtPromocode;
    private final TextButton btnSignIn;
    private final TextButton btnSignUp;
    private final TextButton btnBack;
    private final TextButton btnOkSignIn;
    private final TextButton btnOkSignUp;
    private final CheckBox chkPromocode;
    private final Label lblName;
    private final Label lblPassword;
    private final Label lblEmail;
    private final Image imgValid;
    private final Drawable textureValid;
    private final Drawable textureInvalid;

    private enum CurDialog {Start, SignIn, SignUp}

    private CurDialog curDialog = CurDialog.Start;

    public ScreenLogin(RushClient game, final Model model, PsObject psObject, Skin skin) {
        assert game != null && model != null && skin != null;
        this.game = game;
        this.model = model;
        this.psObject = psObject; // may be NULL

        table.setFillParent(true);
        stage.addActor(table);

        TextureAtlas atlasMenu = new TextureAtlas(Gdx.files.internal("pack/menu.pack"));
        TextureRegion regionValid = atlasMenu.findRegion("valid");
        TextureRegion regionInvalid = atlasMenu.findRegion("invalid");
        assert regionValid != null && regionInvalid != null;
        textureValid = new TextureRegionDrawable(regionValid);
        textureInvalid = new TextureRegionDrawable(regionInvalid);

        txtLogin = new TextField("", skin, "default"); // ....
        txtPassword = new TextField("", skin, "default") {{
            setPasswordMode(true);
            setPasswordCharacter('*');
        }};
        txtEmail = new TextField("", skin, "default");
        txtPromocode = new TextField("", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.checkPromocode(txtPromocode.getText());
                }
            });
        }};
        btnSignIn = new TextButton("Sign in", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSignInDialog(false);
                }
            });
        }};
        btnSignUp = new TextButton("Sign up", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSignUpDialog(false, false);
                }
            });
        }};
        btnBack = new TextButton("Back", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setStartDialog();
                }
            });
        }};
        btnOkSignIn = new TextButton("OK", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.signIn(txtLogin.getText(), txtPassword.getText());
                }
            });
        }};
        btnOkSignUp = new TextButton("OK", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.signUp(txtLogin.getText(), txtPassword.getText(), txtEmail.getText(), txtPromocode.getText());
                }
            });
        }};
        chkPromocode = new CheckBox(" I have a promo code", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSignUpDialog(false, chkPromocode.isChecked());
                }
            });
        }};
        lblName = new Label("Name", skin, "default");
        lblPassword = new Label("Password", skin, "default");
        lblEmail = new Label("Email", skin, "default");
        imgValid = new Image(textureInvalid);

        // only for Android: handling show/hide OnScreenKeyboard
        if (psObject != null) psObject.setListener(new PsObject.Listener() {
            @Override
            public void onRatioChanged(final float ratio) {
                Gdx.app.postRunnable(new Runnable() { // @mitrakov: it is necessary to avoid OutOfSync exceptions!
                    @Override
                    public void run() {
                        switch (curDialog) {
                            case SignIn:
                                setSignInDialog(ratio > 2);
                                break;
                            case SignUp:
                                setSignUpDialog(ratio > 2, false);
                                break;
                            default:
                        }
                    }
                });
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(.25f, .77f, .81f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();

        if (model.authorized)
            game.setNextScreen();

        imgValid.setDrawable(model.promocodeValid ? textureValid : textureInvalid); // if not changed, setter returns

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
        Gdx.input.setInputProcessor(stage);
        setStartDialog();
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (textureValid instanceof TextureRegionDrawable)
            ((TextureRegionDrawable)textureValid).getRegion().getTexture().dispose(); // no NULL references here
        if (textureInvalid instanceof TextureRegionDrawable)
            ((TextureRegionDrawable)textureInvalid).getRegion().getTexture().dispose(); // no NULL references here
    }

    private void setStartDialog() {
        curDialog = CurDialog.Start;
        chkPromocode.setChecked(false);              // clear checking
        Gdx.input.setOnscreenKeyboardVisible(false); // hide keyboard on Android

        table.clear();
        table.add(btnSignIn).width(300).height(80).space(30);
        table.row();
        table.add(btnSignUp).width(300).height(80);
    }

    private void setSignInDialog(boolean shiftForKeyboard) {
        curDialog = CurDialog.SignIn;
        Actor focused = stage.getKeyboardFocus();

        table.clear();
        table.row().space(20);
        table.add(lblName).align(Align.left);
        table.add(txtLogin);
        table.row().space(20);
        table.add(lblPassword).align(Align.left);
        table.add(txtPassword);
        table.row().spaceTop(30);
        table.add(btnBack).width(100).height(40);
        table.add(btnOkSignIn).width(100).height(40);
        if (shiftForKeyboard) shiftUp();

        stage.setKeyboardFocus(focused);
    }

    private void setSignUpDialog(boolean shiftForKeyboard, boolean havePromocode) {
        curDialog = CurDialog.SignUp;
        Actor focused = stage.getKeyboardFocus();

        table.clear();
        table.row().space(20);
        table.add(lblName).align(Align.left);
        table.add(txtLogin);
        table.row().space(20);
        table.add(lblPassword).align(Align.left);
        table.add(txtPassword);
        table.row().space(20);
        table.add(lblEmail).align(Align.left);
        table.add(txtEmail);
        table.row().space(20);
        table.add(chkPromocode);
        if (havePromocode) {
            table.add(txtPromocode);
            table.add(imgValid).spaceLeft(20);
        }
        table.row().spaceTop(30);
        table.add(btnBack).width(100).height(40);
        table.add(btnOkSignUp).width(100).height(40);
        if (shiftForKeyboard) shiftUp();

        stage.setKeyboardFocus(focused);
    }

    private void shiftUp() {
        table.row().spaceTop(200);
        table.add(new Image());
    }
}
