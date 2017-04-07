package ru.mitrakov.self.rush.dialogs;

import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import ru.mitrakov.self.rush.ui.*;
import ru.mitrakov.self.rush.model.Model;
import ru.mitrakov.self.rush.AudioManager;

/**
 * Created by mitrakov on 05.03.2017
 */

public class DialogFriends extends DialogFeat {
    private final TextButton btnInvite;
    private final TextButton btnRemove;
    private String name = "";

    public DialogFriends(final Model model, Skin skin, String style, final DialogInvite invDialog,
                         final DialogQuestion yesNoDialog, final Stage stage, AudioManager audioManager) {
        super("", skin, style);
        assert model != null && invDialog != null && yesNoDialog != null && stage != null;

        btnInvite = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    hide();
                    invDialog.setArguments(DialogInvite.InviteType.ByName, name).show(stage);
                }
            });
        }};
        btnRemove = new TextButtonFeat("", skin, "default", audioManager) {{
            addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    hide();
                    yesNoDialog.setText(String.format("Do you wanna remove %s?", name)).setRunnable(new Runnable() {
                        @Override
                        public void run() {
                            model.removeFriend(name);
                        }
                    }).show(stage);
                }
            });
        }};

        Table table = getContentTable();
        assert table != null;
        table.add(btnInvite).width(200);
        table.row();
        table.add(btnRemove).width(200);

        padTop(0);
        button("Close");
    }

    public Dialog setFriend(String name) {
        assert name != null;
        this.name = name;
        btnInvite.setText(String.format("Invite %s", name));
        btnRemove.setText(String.format("Remove %s", name));
        return this;
    }
}
