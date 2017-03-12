package ru.mitrakov.self.rush;

import java.util.Locale;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;

/**
 * Created by mitrakov on 05.03.2017
 */

class DialogFinished extends Dialog {
    private final RushClient game;
    private final Label lblMessage1;
    private final Label lblMessage2;
    private final Label lblScore;

    private boolean quitOnResult = false;

    DialogFinished(RushClient game, Skin skin, String windowStyleName) {
        super("Round finished", skin, windowStyleName);
        assert game != null;
        this.game = game;
        lblMessage1 = new Label("", skin, "default");
        lblMessage2 = new Label("", skin, "default");
        lblScore = new Label("", skin, "default");

        init(getContentTable(), skin);
        button("OK");
    }

    @Override
    protected void result(Object object) {
        if (quitOnResult) {
            game.setNextScreen();
            hide(null); // default hiding uses fadeout Action 400 ms long that may be undesirable when screens change
        }
    }

    DialogFinished setText(String text1, String text2) {
        assert text1 != null && text2 != null;
        lblMessage1.setText(text1);
        lblMessage2.setText(text2);
        return this;
    }

    DialogFinished setScore(int score1, int score2) {
        lblScore.setText(String.format(Locale.getDefault(), "%d - %d", score1, score2));
        return this;
    }

    DialogFinished setQuitOnResult(boolean value) {
        quitOnResult = value;
        return this;
    }

    private void init(Table table, Skin skin) {
        assert table != null;
        table.row().space(10);
        table.add(lblMessage1);
        table.row().space(10);
        table.add(lblMessage2);
        table.row().space(10);
        table.add(new Label("Total score:", skin, "default"));
        table.row().space(10);
        table.add(lblScore);
        table.row().space(10).width(400);
        table.add(new Label("", skin, "default")); // to stretch the window up to width=400
    }
}
