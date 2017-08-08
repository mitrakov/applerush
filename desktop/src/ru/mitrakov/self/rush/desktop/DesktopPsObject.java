package ru.mitrakov.self.rush.desktop;

import java.awt.*;
import java.util.Locale;

import javax.swing.*;

import com.badlogic.gdx.Gdx;

import ru.mitrakov.self.rush.*;

import static javax.swing.JOptionPane.*;

/**
 * Created by mitrakov on 26.07.2017
 */
class DesktopPsObject extends PsObject {
    private final JFrame parent;
    private final Icon icon;
    private JDialog dialog;

    DesktopPsObject(IBillingProvider billingProvider, JFrame parent, Icon icon) {
        super(billingProvider);
        this.parent = parent;
        this.icon = icon;
    }

    @Override
    public String getPlatform() {
        return String.format(Locale.getDefault(), "W.%s.%d", Gdx.app.getType(), Gdx.app.getVersion());
    }

    @Override
    public void hide() {
    }

    @Override
    public void setActive(boolean value) {
        if (value && dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }

    @Override
    public void pushNotification(String msg) {
        if (!parent.isVisible()) { // if the app is active => no need to push notifications
            JOptionPane pane = new JOptionPane(msg, QUESTION_MESSAGE, OK_CANCEL_OPTION, icon);
            dialog = pane.createDialog(null, "Winesaps");
            locateToRightBottom(dialog);
            Object sound = Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.default"); //TODO only on Windows?
            if (sound instanceof Runnable)
                ((Runnable) sound).run();
            dialog.setVisible(true); // this call blocks the execution!
            dialog.dispose();
            Object value = pane.getValue();
            if (value instanceof Integer)
                if ((Integer) value == OK_OPTION)
                    parent.setVisible(true);
        }
    }

    @Override
    public String getKeyboardVendor() {
        return "";
    }

    /**
     * See http://stackoverflow.com/questions/9753722
     *
     * @param f - component to locate
     */
    private void locateToRightBottom(Component f) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - f.getWidth();
        int y = (int) rect.getMaxY() - f.getHeight() - 50;
        f.setLocation(x, y);
    }
}
