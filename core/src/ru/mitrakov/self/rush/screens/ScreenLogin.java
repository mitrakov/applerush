package ru.mitrakov.self.rush.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;

import ru.mitrakov.self.rush.*;
import ru.mitrakov.self.rush.ui.*;
import ru.mitrakov.self.rush.model.Model;

/**
 * Created by mitrakov on 01.03.2017
 */
public class ScreenLogin extends LocalizableScreen {

    private final Table tableMain = new Table();
    private final TextureAtlas atlasMenu = new TextureAtlas(Gdx.files.internal("pack/menu.pack"));
    private final TextField txtLogin;
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
    private final Drawable textureEng;
    private final Drawable textureRus;
    private final Drawable textureValid;
    private final Drawable textureInvalid;

    private enum CurDialog {Start, SignIn, SignUp}

    private CurDialog curDialog = CurDialog.Start;
    private boolean shiftedByKeyboard = false;

    public ScreenLogin(RushClient game, final Model model, PsObject psObject, Skin skin, AudioManager audioManager) {
        super(game, model, psObject, skin, audioManager);

        TextureRegion regionEng = atlasMenu.findRegion("eng");
        TextureRegion regionRus = atlasMenu.findRegion("rus");
        TextureRegion regionValid = atlasMenu.findRegion("valid");
        TextureRegion regionInvalid = atlasMenu.findRegion("invalid");
        assert regionValid != null && regionInvalid != null && regionEng != null && regionRus != null;
        textureEng = new TextureRegionDrawable(regionEng);
        textureRus = new TextureRegionDrawable(regionRus);
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
        btnSignIn = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSignInDialog();
                }
            });
        }};
        btnSignUp = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSignUpDialog(false);
                }
            });
        }};
        btnBack = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setStartDialog();
                }
            });
        }};
        btnOkSignIn = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.signIn(txtLogin.getText(), txtPassword.getText());
                }
            });
        }};
        btnOkSignUp = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.signUp(txtLogin.getText(), txtPassword.getText(), txtEmail.getText(), txtPromocode.getText());
                }
            });
        }};
        chkPromocode = new CheckBox("", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSignUpDialog(chkPromocode.isChecked());
                }
            });
        }};
        lblName = new Label("", skin, "default");
        lblPassword = new Label("", skin, "default");
        lblEmail = new Label("", skin, "default");
        imgValid = new Image(textureInvalid);

        // set up layout
        table.add(createLangTable(audioManager)).right();
        table.row();
        table.add(tableMain).expand();

        // only for Android: handling show/hide OnScreenKeyboard
        if (psObject != null) psObject.setRatioListener(new PsObject.RatioListener() {
            @Override
            public void onRatioChanged(final float ratio) {
                Gdx.app.postRunnable(new Runnable() { // @mitrakov: it is necessary to avoid OutOfSync exceptions!
                    @Override
                    public void run() {
                        shiftedByKeyboard = ratio > 2;
                        switch (curDialog) {
                            case SignIn:
                                setSignInDialog();
                                break;
                            case SignUp:
                                setSignUpDialog(chkPromocode.isChecked());
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
        super.render(delta);

        if (model.authorized)
            game.setNextScreen();
        imgValid.setDrawable(model.promocodeValid ? textureValid : textureInvalid); // if not changed, setter returns
    }

    @Override
    public void show() {
        super.show();
        setStartDialog();
    }

    @Override
    public void dispose() {
        atlasMenu.dispose(); // disposing an atlas also disposes all its internal textures
        super.dispose();
    }

    @Override
    public void onLocaleChanged(I18NBundle bundle) {
        super.onLocaleChanged(bundle);
        assert bundle != null;

        btnSignIn.setText(bundle.format("sign.in"));
        btnSignUp.setText(bundle.format("sign.up"));
        btnBack.setText(bundle.format("back"));
        btnOkSignIn.setText(bundle.format("ok"));
        btnOkSignUp.setText(bundle.format("ok"));
        chkPromocode.setText(bundle.format("sign.promocode"));
        lblName.setText(bundle.format("sign.name"));
        lblPassword.setText(bundle.format("sign.password"));
        lblEmail.setText(bundle.format("sign.email"));
    }

    private Actor createLangTable(AudioManager audioManager) {
        assert audioManager != null;
        Table tableLang = new Table().padRight(20);
        tableLang.add(new ImageButtonFeat(textureEng, audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.languageEn = true;
                    game.updateLocale();
                }
            });
        }}).spaceRight(20);
        tableLang.add(new ImageButtonFeat(textureRus, audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.languageEn = false;
                    game.updateLocale();
                }
            });
        }}).spaceRight(20);
        return tableLang;
    }

    private void setStartDialog() {
        curDialog = CurDialog.Start;
        chkPromocode.setChecked(false);              // clear checking
        Gdx.input.setOnscreenKeyboardVisible(false); // hide keyboard on Android

        tableMain.clear();
        tableMain.add(btnSignIn).width(300).height(80).space(30);
        tableMain.row();
        tableMain.add(btnSignUp).width(300).height(80);
    }

    private void setSignInDialog() {
        curDialog = CurDialog.SignIn;
        Actor focused = stage.getKeyboardFocus();

        tableMain.clear();
        tableMain.row().space(20);
        tableMain.add(lblName).align(Align.left);
        tableMain.add(txtLogin);
        tableMain.row().space(20);
        tableMain.add(lblPassword).align(Align.left);
        tableMain.add(txtPassword);
        tableMain.row().spaceTop(30);
        tableMain.add(btnBack).width(100).height(40);
        tableMain.add(btnOkSignIn).width(100).height(40);
        if (shiftedByKeyboard) shiftUp();

        stage.setKeyboardFocus(focused);
    }

    private void setSignUpDialog(boolean havePromocode) {
        curDialog = CurDialog.SignUp;
        Actor focused = stage.getKeyboardFocus();

        txtPromocode.setVisible(havePromocode);
        imgValid.setVisible(havePromocode);

        tableMain.clear();
        tableMain.row().space(20);
        tableMain.add(lblName).align(Align.left);
        tableMain.add(txtLogin);
        tableMain.row().space(20);
        tableMain.add(lblPassword).align(Align.left);
        tableMain.add(txtPassword);
        tableMain.row().space(20);
        tableMain.add(lblEmail).align(Align.left);
        tableMain.add(txtEmail);
        tableMain.row().space(20);
        tableMain.add(chkPromocode);
        tableMain.add(txtPromocode);
        tableMain.add(imgValid).spaceLeft(20);
        tableMain.row().spaceTop(30);
        tableMain.add(btnBack).width(100).height(40);
        tableMain.add(btnOkSignUp).width(100).height(40);
        if (shiftedByKeyboard) shiftUp();

        stage.setKeyboardFocus(focused);
    }

    private void shiftUp() {
        tableMain.row().spaceTop(200);
        tableMain.add(new Image());  // 'blank' image to fill space taken by on-screen keyboard
    }
}
