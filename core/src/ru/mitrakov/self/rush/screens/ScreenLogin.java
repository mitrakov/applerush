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
import ru.mitrakov.self.rush.dialogs.DialogInfo;

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
    private final DialogInfo infoDialog;
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
    private I18NBundle i18n;
    private boolean shiftedByKeyboard = false;

    public ScreenLogin(RushClient game, final Model model, PsObject psObject, Skin skin, AudioManager audioManager,
                       I18NBundle i18nb) {
        super(game, model, psObject, skin, audioManager);
        assert i18nb != null;
        i18n = i18nb;

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
                    assert i18n != null;
                    String password = txtPassword.getText();
                    if (password.length() >= 4)
                        model.signUp(txtLogin.getText(), password, txtEmail.getText(), txtPromocode.getText());
                    else infoDialog.setText(i18n.format("dialog.info.incorrect.password")).show(stage);
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
        infoDialog = new DialogInfo("", skin, "default");
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
        this.i18n = bundle;

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

    @Override
    public void handleEvent(EventBus.Event event) {
        assert i18n != null;
        if (event instanceof EventBus.AuthorizedChangedEvent) {
            EventBus.AuthorizedChangedEvent ev = (EventBus.AuthorizedChangedEvent) event;
            if (ev.authorized)
                game.setNextScreen();
        }
        if (event instanceof EventBus.IncorrectCredentialsEvent)
            infoDialog.setText(i18n.format("dialog.info.incorrect.credentials")).show(stage);
        if (event instanceof EventBus.IncorrectNameEvent)
            infoDialog.setText(i18n.format("dialog.info.incorrect.name")).show(stage);
        if (event instanceof EventBus.IncorrectEmailEvent)
            infoDialog.setText(i18n.format("dialog.info.incorrect.email")).show(stage);
        if (event instanceof EventBus.DuplicateNameEvent)
            infoDialog.setText(i18n.format("dialog.info.duplicate.name")).show(stage);
        if (event instanceof EventBus.SignUpErrorEvent)
            infoDialog.setText(i18n.format("dialog.info.incorrect.signup")).show(stage);
    }

    @Override
    public void handleEventBackground(EventBus.Event event) {
        if (event instanceof EventBus.PromocodeValidChanged) {
            EventBus.PromocodeValidChanged ev = (EventBus.PromocodeValidChanged) event;
            imgValid.setDrawable(ev.valid ? textureValid : textureInvalid);
        }
    }

    private Actor createLangTable(AudioManager audioManager) {
        assert audioManager != null;
        Table tableLang = new Table().padRight(10).padTop(10);
        tableLang.add(new ImageButtonFeat(textureEng, audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.languageEn = true;
                    game.updateLocale();
                }
            });
        }}).spaceRight(10);
        tableLang.add(new ImageButtonFeat(textureRus, audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.languageEn = false;
                    game.updateLocale();
                }
            });
        }}).spaceRight(10);
        return tableLang;
    }

    private void setStartDialog() {
        curDialog = CurDialog.Start;
        chkPromocode.setChecked(false);              // clear checking
        Gdx.input.setOnscreenKeyboardVisible(false); // hide keyboard on Android

        tableMain.clear();
        tableMain.add(btnSignIn).width(222).height(85).space(30);
        tableMain.row();
        tableMain.add(btnSignUp).width(222).height(85);
    }

    private void setSignInDialog() {
        curDialog = CurDialog.SignIn;
        Actor focused = stage.getKeyboardFocus();

        Table buttons = new Table();
        buttons.add(btnBack).width(120).height(46).space(20);
        buttons.add(btnOkSignIn).width(120).height(46).space(20);

        tableMain.clear();
        tableMain.row().space(20);
        tableMain.add(lblName).align(Align.left);
        tableMain.add(txtLogin).width(305).height(50);
        tableMain.row().space(20);
        tableMain.add(lblPassword).align(Align.left);
        tableMain.add(txtPassword).width(305).height(50);
        tableMain.row().spaceTop(30);
        tableMain.add(buttons).colspan(2);
        if (shiftedByKeyboard) shiftUp();

        stage.setKeyboardFocus(focused);
    }

    private void setSignUpDialog(boolean havePromocode) {
        curDialog = CurDialog.SignUp;
        Actor focused = stage.getKeyboardFocus();

        txtPromocode.setVisible(havePromocode);
        imgValid.setVisible(havePromocode);

        Table buttons = new Table();
        buttons.add(btnBack).width(120).height(46).space(20);
        buttons.add(btnOkSignUp).width(120).height(46).space(20);

        tableMain.clear();
        tableMain.row().space(20);
        tableMain.add(lblName).align(Align.left);
        tableMain.add(txtLogin).width(305).height(50).colspan(2);
        tableMain.row().space(20);
        tableMain.add(lblPassword).align(Align.left);
        tableMain.add(txtPassword).width(305).height(50).colspan(2);
        tableMain.row().space(20);
        tableMain.add(lblEmail).align(Align.left);
        tableMain.add(txtEmail).width(305).height(50).colspan(2);
        tableMain.row().space(20);
        tableMain.add(chkPromocode);
        tableMain.add(txtPromocode).width(240).height(50).left();
        tableMain.add(imgValid).width(imgValid.getWidth()).height(imgValid.getHeight());
        tableMain.row().spaceTop(30);
        tableMain.add(buttons).colspan(3);
        if (shiftedByKeyboard) shiftUp();

        stage.setKeyboardFocus(focused);
    }

    private void shiftUp() {
        tableMain.row().spaceTop(200);
        tableMain.add(new Image());  // 'blank' image to fill space taken by on-screen keyboard
    }
}
