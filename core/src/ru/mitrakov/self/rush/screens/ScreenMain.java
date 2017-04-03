package ru.mitrakov.self.rush.screens;

import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

import ru.mitrakov.self.rush.model.*;
import ru.mitrakov.self.rush.PsObject;
import ru.mitrakov.self.rush.dialogs.*;
import ru.mitrakov.self.rush.RushClient;
import ru.mitrakov.self.rush.ui.LinkedLabel;


/**
 * Created by mitrakov on 03.03.2017
 */

public class ScreenMain extends ScreenAdapter {
    private final RushClient game;
    private final Model model;
    private final PsObject psObject;
    private final Stage stage = new Stage(new FitViewport(RushClient.WIDTH, RushClient.HEIGHT));
    private final TextureAtlas atlasAbility = new TextureAtlas(Gdx.files.internal("pack/ability.pack"));
    private final TextureAtlas atlasMenu = new TextureAtlas(Gdx.files.internal("pack/menu.pack"));

    private final Table tableMain = new Table();
    private final Table tableLeft = new Table();
    private final Table tableRight = new Table();
    private final Table tableLeftInvite = new Table();
    private final Table tableLeftToolbar = new Table();
    private final Table tableRightHeader = new Table();
    private final Table tableRightContent = new Table();
    private final Table tableRightContentAbilities = new Table();
    private final Table tableRightContentRatingBtns = new Table();
    private final Table tableRightContentRating = new Table();
    private final Table tableFriendsControl = new Table();
    private final Dialog moreCrystalsDialog;
    private final Dialog incomingDialog;
    private final Dialog settingsDialog;
    private final Dialog aboutDialog;
    private final DialogBuyAbilities buyAbilitiesDialog;
    private final DialogInfo infoDialog;
    private final DialogDialup dialupDialog;
    private final DialogInvite inviteDialog;
    private final DialogFriends friendsDialog;
    private final DialogPromocodeDone promocodeDoneDialog;
    private final List<String> lstHistory;
    private final List<String> lstFriends;
    private final ScrollPane lstHistoryScroll;
    private final ScrollPane lstFriendsScroll;
    private final ScrollPane tableRightContentAbilitiesScroll;
    private final TextField txtEnemyName;
    private final TextField txtFriendName;
    private final Button btnInviteByName;
    private final Button btnInviteRandom;
    private final Button btnInviteLatest;
    private final Button btnInviteByNameOk;
    private final Button btnInviteByNameCancel;
    private final Button btnSettings;
    private final Button btnAbout;
    private final Button btnInfo;
    private final Button btnRating;
    private final Button btnHistory;
    private final Button btnFriends;
    private final Button btnBuyAbilities;
    private final Button btnGeneralRating;
    private final Button btnWeeklyRating;
    private final Button btnAddFriend;
    private final Button btnAddFriendOk;
    private final Button btnAddFriendCancel;
    private final LinkedLabel lblMore;
    private final Label lblName;
    private final Label lblCrystalsHeader;
    private final Label lblCrystalsData;
    private final Label lblAbilities;
    private final Label lblAbilityExpireTime;
    private final Label lblRatingName;
    private final Label lblRatingWins;
    private final Label lblRatingLosses;
    private final Label lblRatingScoreDiff;
    private final Label lblRatingDots;

    private final ObjectMap<Model.Ability, ImageButton> abilities = new ObjectMap<Model.Ability, ImageButton>(10);
    private final Array<Label> ratingLabels = new Array<Label>(4 * (Model.RATINGS_COUNT + 1));

    private enum CurDisplayMode {Info, Rating, History, Friends}

    private long generalRatingTime = 0;
    private long weeklyRatingTime = 0;
    private long inviteTime = 0;
    private long stopCallRejectedTime = 0;
    private long stopCallMissedTime = 0;
    private long stopCallExpiredTime = 0;
    private long friendsListTime = 0;
    private long abilityExpireTime = 0;
    private long promocodeDoneTime = 0;

    public ScreenMain(RushClient game, final Model model, PsObject psObject, Skin skin) {
        assert game != null && model != null && skin != null;
        this.game = game;
        this.model = model;
        this.psObject = psObject; // may be NULL

        tableMain.setFillParent(true);
        stage.addActor(tableMain);

        TextureRegion regionSettings = atlasMenu.findRegion("settings");
        TextureRegion regionAbout = atlasMenu.findRegion("about");
        assert regionSettings != null && regionAbout != null;

        moreCrystalsDialog = new DialogMoreCrystals(skin, "default", new DialogPromocode(model, skin, "default"),
                stage);
        incomingDialog = new DialogIncoming(model, skin, "default");
        settingsDialog = new DialogSettings(model, skin, "default");
        aboutDialog = new DialogAbout(skin, "default");
        buyAbilitiesDialog = new DialogBuyAbilities(model, skin, "default");
        infoDialog = new DialogInfo("Information", skin, "default");
        dialupDialog = new DialogDialup(model, skin, "default");
        inviteDialog = new DialogInvite(model, skin, "default", dialupDialog, stage);
        friendsDialog = new DialogFriends(model, skin, "default", inviteDialog,
                new DialogQuestion("Confirm action", skin, "default"), stage);
        promocodeDoneDialog = new DialogPromocodeDone(skin, "default");
        lstHistory = new List<String>(skin, "default");
        lstFriends = new List<String>(skin, "default") {{
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    String friend = lstFriends.getSelected();
                    if (friend != null)
                        friendsDialog.setFriend(friend).show(stage);
                }
            });
        }};
        lstHistoryScroll = new ScrollPane(lstHistory, skin, "default");
        lstFriendsScroll = new ScrollPane(lstFriends, skin, "default");
        tableRightContentAbilitiesScroll = new ScrollPane(tableRightContentAbilities);
        txtEnemyName = new TextField("", skin, "default");
        txtFriendName = new TextField("", skin, "default");
        btnInviteByName = new TextButton("Find opponent", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildLeftTable(true);
                }
            });
        }};
        btnInviteRandom = new TextButton("Random opponent", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    inviteDialog.setArguments(DialogInvite.InviteType.Random, "").show(stage);
                }
            });
        }};
        btnInviteLatest = new TextButton("Latest opponent", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    inviteDialog.setArguments(DialogInvite.InviteType.Latest, "").show(stage);
                }
            });
        }};
        btnInviteByNameOk = new TextButton("OK", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String name = txtEnemyName.getText();
                    if (name.length() > 0) { // use 'length() > 0' instead of 'isEmpty()' (Android API 8)
                        inviteDialog.setArguments(DialogInvite.InviteType.ByName, name).show(stage);
                        rebuildLeftTable(false);
                    }
                }
            });
        }};
        btnInviteByNameCancel = new TextButton("Cancel", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildLeftTable(false);
                }
            });
        }};
        btnSettings = new ImageButton(new TextureRegionDrawable(regionSettings)) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    settingsDialog.show(stage);
                }
            });
        }};
        btnAbout = new ImageButton(new TextureRegionDrawable(regionAbout)) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    aboutDialog.show(stage);
                }
            });
        }};
        btnInfo = new TextButton("Info", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildRightTable(CurDisplayMode.Info);
                }
            });
        }};
        btnRating = new TextButton("Rating", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildRightTable(CurDisplayMode.Rating);
                }
            });
        }};
        btnHistory = new TextButton("History", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildRightTable(CurDisplayMode.History);
                }
            });
        }};
        btnFriends = new TextButton("Friends", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildRightTable(CurDisplayMode.Friends);
                }
            });
        }};
        btnBuyAbilities = new TextButton("Buy abilities", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    buyAbilitiesDialog.show(stage);
                }
            });
        }};
        btnGeneralRating = new TextButton("General Rating", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.getRating(Model.RatingType.General);
                }
            });
        }};
        btnWeeklyRating = new TextButton("Weekly Rating", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.getRating(Model.RatingType.Weekly);
                }
            });
        }};
        btnAddFriend = new TextButton("Add new", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildFriends(true);
                }
            });
        }};
        btnAddFriendOk = new TextButton("OK", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    model.addFriend(txtFriendName.getText());
                    rebuildFriends(false);
                }
            });
        }};
        btnAddFriendCancel = new TextButton("Cancel", skin, "default") {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuildFriends(false);
                }
            });
        }};
        lblMore = new LinkedLabel("Get ", "more crystals", "", skin, "default", new Runnable() {
            @Override
            public void run() {
                moreCrystalsDialog.show(stage);
            }
        });
        lblName = new Label("", skin, "default");
        lblCrystalsHeader = new Label("Crystals:", skin, "default");
        lblCrystalsData = new Label("", skin, "default");
        lblAbilities = new Label("Abilities:", skin, "default");
        lblAbilityExpireTime = new Label("", skin, "default");
        lblRatingName = new Label("Name", skin, "default");
        lblRatingWins = new Label("Wins", skin, "default");
        lblRatingLosses = new Label("Losses", skin, "default");
        lblRatingScoreDiff = new Label("Score diff", skin, "default");
        lblRatingDots = new Label(". . .", skin, "default");

        loadTextures();
        initTables(skin);
        rebuildLeftTable(false);
        rebuildRightTable(CurDisplayMode.Info);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(.25f, .77f, .81f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();

        lblName.setText(model.name); // if text is not changed, setText just returns
        lblCrystalsData.setText(String.valueOf(model.crystals));

        // updating internal state
        updateInvite();
        updateRatings();
        updateFriends();
        updateStopCall();
        updateAbilities();
        updatePromocodeDone();

        // changing screens
        if (!model.authorized)
            game.setLoginScreen();
        if (model.field != null) {
            dialupDialog.hide();
            game.setNextScreen();
        }

        // checking BACK and MENU buttons on Android
        if (psObject != null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK))
                psObject.hide();
            if (Gdx.input.isKeyJustPressed(Input.Keys.MENU))
                settingsDialog.show(stage);
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        generalRatingTime = model.generalRatingTime;
        weeklyRatingTime = model.weeklyRatingTime;
        inviteTime = model.inviteTime;
        stopCallRejectedTime = model.stopCallRejectedTime;
        stopCallMissedTime = model.stopCallMissedTime;
        stopCallExpiredTime = model.stopCallExpiredTime;
        friendsListTime = model.friendsListTime;
        abilityExpireTime = model.abilityExpireTime;
        promocodeDoneTime = model.promocodeDoneTime;
    }

    @Override
    public void dispose() {
        stage.dispose();
        atlasAbility.dispose(); // disposing an atlas also disposes all its internal textures
        atlasMenu.dispose();
        buyAbilitiesDialog.dispose();
        super.dispose();
    }

    private void loadTextures() {
        for (final Model.Ability ability : Model.Ability.values()) {
            TextureRegion region = atlasAbility.findRegion(ability.name());
            if (region != null) {
                ImageButton imageButton = new ImageButton(new TextureRegionDrawable(region));
                imageButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Integer minutes = model.abilityExpireMap.get(ability); // count of minutes at INITIAL time!
                        if (minutes != null) {
                            long minLeft = minutes - (TimeUtils.millis() - model.abilityExpireTime) / 60000;
                            if (minLeft < 0) // if server's expire checking period is too large, value may be < 0
                                minLeft = 0;
                            lblAbilityExpireTime.setText(String.format(Locale.getDefault(), "time left: %02d:%02d",
                                    minLeft / 60, minLeft % 60));
                            lblAbilityExpireTime.clearActions();
                            lblAbilityExpireTime.addAction(sequence(fadeIn(.1f), Actions.show(),
                                    fadeOut(2, Interpolation.fade), Actions.hide()));
                        }
                    }
                });
                abilities.put(ability, imageButton);
            }
        }
    }

    private void initTables(Skin skin) {
        tableMain.add(tableLeft).pad(20).fill();
        tableMain.add(tableRight).pad(20).expand().fill();

        tableLeft.add(tableLeftInvite).expand().fill();
        tableLeft.row();
        tableLeft.add(tableLeftToolbar);

        tableRight.add(tableRightHeader);
        tableRight.row();
        tableRight.add(tableRightContent).expand().fill();

        tableRightHeader.row().width(90).space(20);
        tableRightHeader.add(btnInfo);
        tableRightHeader.add(btnRating);
        tableRightHeader.add(btnHistory);
        tableRightHeader.add(btnFriends);

        tableRightContentRatingBtns.row().spaceLeft(30);
        tableRightContentRatingBtns.add(btnGeneralRating);
        tableRightContentRatingBtns.add(btnWeeklyRating);

        tableRightContentRating.row().spaceLeft(20).spaceBottom(20); // don't use space(), it breaks the layout
        tableRightContentRating.add(lblRatingName);
        tableRightContentRating.add(lblRatingWins);
        tableRightContentRating.add(lblRatingLosses);
        tableRightContentRating.add(lblRatingScoreDiff);

        final int RATING_COLUMNS = 4;
        for (int i = 0; i < Model.RATINGS_COUNT; i++) {
            tableRightContentRating.row().spaceLeft(20);
            for (int j = 0; j < RATING_COLUMNS; j++) {
                Label label = new Label("", skin, "default");
                tableRightContentRating.add(label);
                ratingLabels.add(label);
            }
        }
        tableRightContentRating.row().spaceLeft(20);
        tableRightContentRating.add(lblRatingDots).colspan(4);
        tableRightContentRating.row().spaceLeft(20);
        for (int j = 0; j < RATING_COLUMNS; j++) {
            Label label = new Label("", skin, "default");
            tableRightContentRating.add(label);
            ratingLabels.add(label);
        }
    }

    private void rebuildLeftTable(boolean showInputName) {
        Gdx.input.setOnscreenKeyboardVisible(false); // hide keyboard on Android
        tableLeftInvite.clear();

        // ...
        if (showInputName) {
            tableLeftInvite.add(txtEnemyName).colspan(2).width(140).height(50);
            tableLeftInvite.row().space(20);
            tableLeftInvite.add(btnInviteByNameOk).width(60).height(40);
            tableLeftInvite.add(btnInviteByNameCancel).width(80).height(40);
            tableLeftInvite.row().space(20);
            tableLeftInvite.add(btnInviteRandom).colspan(2).width(160).height(80);
            tableLeftInvite.row().space(20);
            tableLeftInvite.add(btnInviteLatest).colspan(2).width(160).height(80);
        } else {
            tableLeftInvite.add(btnInviteByName).width(160).height(80);
            tableLeftInvite.row().space(20);
            tableLeftInvite.add(btnInviteRandom).width(160).height(80);
            tableLeftInvite.row().space(20);
            tableLeftInvite.add(btnInviteLatest).width(160).height(80);
        }

        tableLeftToolbar.add(btnSettings).spaceRight(30);
        tableLeftToolbar.add(btnAbout);
    }

    private void rebuildRightTable(CurDisplayMode mode) {
        tableRightContent.clear();

        switch (mode) {
            case Info:
                tableRightContent.add(lblName).colspan(2).expand();
                tableRightContent.row().expand();
                tableRightContent.add(lblCrystalsHeader);
                tableRightContent.add(lblCrystalsData);
                tableRightContent.row().expand();
                tableRightContent.add(lblAbilities);
                tableRightContent.add(tableRightContentAbilitiesScroll).pad(15);
                tableRightContent.row().expandX();
                tableRightContent.add();
                tableRightContent.add(lblAbilityExpireTime);
                tableRightContent.row().expand();
                tableRightContent.add(btnBuyAbilities).colspan(2);
                tableRightContent.row().expand();
                tableRightContent.add(lblMore).colspan(2);
                break;
            case Rating:
                tableRightContent.add(tableRightContentRatingBtns).pad(15);
                tableRightContent.row();
                tableRightContent.add(tableRightContentRating).expand();
                model.getRating(Model.RatingType.General); // we should requery rating each time we choose the tab,
                break;                                     // because it might be updated on the server
            case History:
                lstHistory.setItems(model.history.toArray(new String[0]));
                tableRightContent.add(lstHistoryScroll).fill(.9f, .9f).expand();
                break;
            case Friends:
                tableRightContent.add(tableFriendsControl).pad(15);
                tableRightContent.row();
                tableRightContent.add(lstFriendsScroll).fill(.9f, .9f).expand();
                rebuildFriends(false);
                model.getFriends(); // we should requery friends each time we choose the tab, because friends may be
                break;              // added from different places
            default:
        }
    }

    private void rebuildFriends(boolean showInputName) {
        tableFriendsControl.clear();

        if (showInputName) {
            tableFriendsControl.add(txtFriendName).width(180).colspan(2);
            tableFriendsControl.row().space(10);
            tableFriendsControl.add(btnAddFriendOk).width(70);
            tableFriendsControl.add(btnAddFriendCancel).width(100);
        } else {
            tableFriendsControl.add(btnAddFriend);
        }
    }

    private void updateAbilities() {
        if (abilityExpireTime != model.abilityExpireTime) {
            abilityExpireTime = model.abilityExpireTime;
            tableRightContentAbilities.clear();
            // we sort abilities (via TreeSet), because since 2017.04.03 'model.abilityExpireMap' is not SkipListMap
            for (Model.Ability ability : new TreeSet<Model.Ability>(model.abilityExpireMap.keySet())) {
                ImageButton btn = abilities.get(ability);
                if (btn != null)
                    tableRightContentAbilities.add(btn).space(10);
            }
        }
    }

    private void updateRatings() {
        if (generalRatingTime != model.generalRatingTime) {
            generalRatingTime = model.generalRatingTime;
            updateRating(Model.RatingType.General);
        } else if (weeklyRatingTime != model.weeklyRatingTime) {
            weeklyRatingTime = model.weeklyRatingTime;
            updateRating(Model.RatingType.Weekly);
        }
    }

    private void updateRating(Model.RatingType type) {
        for (Label label : ratingLabels) {
            label.setText("");
        }

        Collection<RatingItem> items = type == Model.RatingType.General ? model.generalRating : model.weeklyRating;
        int i = 0;
        for (RatingItem item : items) {
            if (i + 3 < ratingLabels.size) {
                ratingLabels.get(i++).setText(item.name);
                ratingLabels.get(i++).setText(String.valueOf(item.victories));
                ratingLabels.get(i++).setText(String.valueOf(item.defeats));
                ratingLabels.get(i++).setText(String.valueOf(item.score_diff));
            }
        }
    }

    private void updateFriends() {
        if (friendsListTime != model.friendsListTime) {
            friendsListTime = model.friendsListTime;
            lstFriends.setItems(model.friends.toArray(new String[0]));
        }
    }

    private void updateInvite() {
        if (inviteTime != model.inviteTime) {
            inviteTime = model.inviteTime;
            incomingDialog.show(stage);
        }
    }

    private void updatePromocodeDone() {
        if (promocodeDoneTime != model.promocodeDoneTime) {
            promocodeDoneTime = model.promocodeDoneTime;
            promocodeDoneDialog.setArguments(model.promocodeDoneName, model.promocodeDoneInviter,
                    model.promocodeDoneCrystals).show(stage);
        }
    }

    private void updateStopCall() {
        if (stopCallRejectedTime != model.stopCallRejectedTime) {
            stopCallRejectedTime = model.stopCallRejectedTime;
            dialupDialog.hide();
            infoDialog.setText(String.format("%s rejected your invitation", model.enemy)).show(stage);
        }
        if (stopCallMissedTime != model.stopCallMissedTime) {
            stopCallMissedTime = model.stopCallMissedTime;
            incomingDialog.hide();
            infoDialog.setText(String.format("You missed invitation from %s", model.enemy)).show(stage);
        }
        if (stopCallExpiredTime != model.stopCallExpiredTime) {
            stopCallExpiredTime = model.stopCallExpiredTime;
            dialupDialog.hide();
            infoDialog.setText(String.format("%s doesn't respond", model.enemy)).show(stage);
        }
    }
}
