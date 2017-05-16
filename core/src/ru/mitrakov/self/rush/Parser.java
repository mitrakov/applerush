package ru.mitrakov.self.rush;

import java.util.*;

import ru.mitrakov.self.rush.model.*;
import ru.mitrakov.self.rush.net.IHandler;

import static ru.mitrakov.self.rush.model.Model.*;
import static ru.mitrakov.self.rush.utils.Utils.copyOfRange;

/**
 * Created by mitrakov on 23.02.2017
 */
class Parser implements IHandler {
    private static final int ERR_SIGNIN_INCORRECT_PASSWORD = 31;
    private static final int ERR_ATTACK_YOURSELF = 50;
    private static final int ERR_AGGRESSOR_BUSY = 51;
    private static final int ERR_DEFENDER_BUSY = 52;
    private static final int ERR_BATTLE_NOT_FOUND = 73;
    private static final int ERR_SIGN_UP = 201;
    private static final int ERR_SIGNIN_INCORRECT_LOGIN = 204;
    private static final int ERR_NO_CRYSTALS = 215;
    private static final int ERR_ADD_FRIEND = 223;
    private static final int ERR_USER_NOT_FOUND = 245;
    private static final int ERR_INCORRECT_TOKEN = 246;
    private static final int ERR_ENEMY_NOT_FOUND = 247;
    private static final int ERR_NO_FREE_USERS = 248;
    private static final int ERR_INCORRECT_NAME = 249;
    private static final int ERR_INCORRECT_EMAIL = 251;
    private static final int ERR_DUPLICATE_NAME = 252;

    private final Model model;
    private final PsObject psObject;

    Parser(Model model, PsObject psObject) {
        assert model != null;
        this.model = model;
        this.psObject = psObject; // may be NULL
    }

    @Override
    public void onReceived(int[] data) {
        // @mitrakov: on Android copyOfRange requires minSdkVersion=9
        assert data != null;

        if (data.length > 0) {
            int code = data[0];
            Cmd[] commands = Cmd.values();
            if (0 <= code && code < commands.length) {
                Cmd cmd = commands[code];
                switch (cmd) {
                    case SIGN_IN:
                    case SIGN_UP:
                        signIn(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case SIGN_OUT:
                        signOut(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case USER_INFO:
                    case BUY_PRODUCT:
                        userInfo(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case ATTACK: // response on Attack
                        attack(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case CALL:
                        call(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case STOPCALL:
                        stopCall(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case FRIEND_LIST:
                        friendList(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case ADD_FRIEND:
                        addFriend(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case REMOVE_FRIEND:
                        removeFriend(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case RANGE_OF_PRODUCTS:
                        rangeOfProducts(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case ROUND_INFO:
                        roundInfo(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case RATING:
                        rating(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case FULL_STATE:
                        fullState(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case STATE_CHANGED:
                        stateChanged(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case SCORE_CHANGED:
                        scoreChanged(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case PLAYER_WOUNDED:
                        playerWounded(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case FINISHED:
                        finished(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case THING_TAKEN:
                        thingTaken(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case ABILITY_LIST:
                        abilitiesList(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case OBJECT_APPENDED:
                        objectAppended(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case CHECK_PROMOCODE:
                        checkPromocode(cmd, copyOfRange(data, 1, data.length));
                        break;
                    case PROMOCODE_DONE:
                        promocodeDone(cmd, copyOfRange(data, 1, data.length));
                        break;
                    default:
                        if (data.length > 1)
                            inspectError(cmd, data[1]);
                        else throw new IllegalArgumentException("Unhandled command code");
                }
            } else throw new IllegalArgumentException("Incorrect command code");
        } else throw new IllegalArgumentException("Empty data");
    }

    @Override
    public void onChanged(boolean connected) {
        model.setConnected(connected);
    }

    private void signIn(Cmd cmd, int[] data) {
        if (data.length == 1) {
            int error = data[0];
            if (error == 0)
                model.setAuthorized(true);
            else inspectError(cmd, error);
        } else throw new IllegalArgumentException("Incorrect sign-in format");
    }

    private void signOut(Cmd cmd, int[] data) {
        if (data.length == 1) {
            int error = data[0];
            if (error == 0)
                model.setAuthorized(false);
            else inspectError(cmd, error);
        } else throw new IllegalArgumentException("Incorrect sign-out format");
    }

    private void userInfo(Cmd cmd, int[] data) {
        if (data.length > 0) {
            int error = data[0];
            if (error == 0)
                model.setUserInfo(copyOfRange(data, 1, data.length));
            else inspectError(cmd, error);
        } else throw new IllegalArgumentException("Incorrect user info format");
    }

    private void attack(Cmd cmd, int[] data) {
        if (data.length > 0) {
            int error = data[0];
            if (error == 0) {
                StringBuilder victim = new StringBuilder(); // in Java 8 may be replaced with a StringJoiner
                for (int i = 1; i < data.length; i++) {
                    victim.append((char) data[i]);
                }
                model.setVictim(victim.toString());
            } else inspectError(cmd, error);
        } else throw new IllegalArgumentException("Incorrect attack format");
    }

    private void call(Cmd cmd, int[] data) {
        if (data.length > 3) {
            int sidH = data[0];
            int sidL = data[1];
            int sid = sidH * 256 + sidL;
            StringBuilder name = new StringBuilder(); // in Java 8 may be replaced with a StringJoiner
            for (int i = 2; i < data.length; i++) {
                name.append((char) data[i]);
            }
            model.attacked(sid, name.toString());
            if (psObject != null && model.notifyNewBattles)
                psObject.activate();
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect call format");
    }

    private void stopCall(Cmd cmd, int[] data) {
        if (data.length > 0) {
            boolean rejected = data[0] == 0;
            boolean missed = data[0] == 1;
            boolean expired = data[0] == 2;
            StringBuilder name = new StringBuilder(); // in Java 8 may be replaced with a StringJoiner
            for (int i = 1; i < data.length; i++) {
                name.append((char) data[i]);
            }
            if (rejected)
                model.stopCallRejected(name.toString());
            else if (missed)
                model.stopCallMissed(name.toString());
            else if (expired)
                model.stopCallExpired(name.toString());
            else inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect stopCall format");
    }

    private void friendList(Cmd cmd, int[] data) {
        if (data.length > 1) {
            int error = data[0];
            int fragNumber = data[1];
            if (error == 0)
                model.setFriendList(copyOfRange(data, 2, data.length), fragNumber > 1);
            else inspectError(cmd, error);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect friend list format");
    }

    private void addFriend(Cmd cmd, int[] data) {
        if (data.length > 0) {
            int error = data[0];
            if (error == 0) {
                if (data.length > 1) {
                    int character = data[1];
                    StringBuilder name = new StringBuilder(); // in Java 8 may be replaced with a StringJoiner
                    for (int i = 2; i < data.length; i++) {
                        name.append((char) data[i]);
                    }
                    model.friendAdded(character, name.toString());
                } else throw new IllegalArgumentException("Incorrect addFriend format");
            } else inspectError(cmd, error);
        } else throw new IllegalArgumentException("Incorrect add friend format");
    }

    private void removeFriend(Cmd cmd, int[] data) {
        if (data.length > 0) {
            int error = data[0];
            if (error == 0) {
                StringBuilder name = new StringBuilder(); // in Java 8 may be replaced with a StringJoiner
                for (int i = 1; i < data.length; i++) {
                    name.append((char) data[i]);
                }
                model.friendRemoved(name.toString());
            } else inspectError(cmd, error);
        } else throw new IllegalArgumentException("Incorrect remove friend format");
    }

    private void rangeOfProducts(Cmd cmd, int[] data) {
        if (data.length % 3 == 0) {
            model.setRangeOfProducts(data);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect range-of-products format");
    }

    private void roundInfo(Cmd cmd, int[] data) {
        if (data.length > 6) {
            int number = data[0];
            int timeSec = data[1];
            boolean aggressor = data[2] != 0;
            int character1 = data[3];
            int character2 = data[4];
            int myLives = data[5];
            int enemyLives = data[6];
            model.setRoundInfo(number, timeSec, aggressor, character1, character2, myLives, enemyLives);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect round info format");
    }

    private void rating(Cmd cmd, int[] data) {
        if (data.length > 1) {
            int error = data[0];
            int type = data[1];
            RatingType[] types = RatingType.values();
            if (error == 0 && (0 <= type && type < types.length)) {
                model.setRating(types[type], copyOfRange(data, 2, data.length));
            } else inspectError(cmd, error);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect rating format");
    }

    private void fullState(Cmd cmd, int[] state) {
        int n = Field.HEIGHT * Field.WIDTH;
        if (state.length >= n) {
            // field
            int field[] = copyOfRange(state, 0, n);
            model.setNewField(field);

            // scanning additional sections
            for (int j = n; j + 1 < state.length; j += 2) {
                int sectionCode = state[j];
                int sectionLen = state[j + 1];
                switch (sectionCode) {
                    case 1: // parse additional level objects
                        int objects[] = copyOfRange(state, j + 2, j + 2 + sectionLen);
                        for (int i = 0; i < objects.length; i += 3) {
                            int number = objects[i];
                            int id = objects[i + 1];
                            int xy = objects[i + 2];
                            model.appendObject(number, id, xy);
                        }
                        break;
                    case 2: // parse style pack
                        if (sectionLen == 1 && j + 2 < state.length) {
                            int pack = state[j + 2];
                            model.setStylePack(pack);
                        } else throw new IllegalArgumentException("Incorrect style pack");
                        break;
                    default: // don't throw exceptions, just skip
                }
                j += sectionLen;
            }
        } else if (state.length == 1) {
            inspectError(cmd, state[0]);
        } else throw new IllegalArgumentException("Incorrect field size");
    }

    private void stateChanged(Cmd cmd, int[] pairs) {
        if (pairs.length % 2 == 0) {
            for (int i = 0; i < pairs.length; i += 2) {
                int number = pairs[i];
                int xy = pairs[i + 1];
                model.setXy(number, xy);
            }
        } else if (pairs.length == 1) {
            inspectError(cmd, pairs[0]);
        } else throw new IllegalArgumentException("Incorrect state changed format");
    }

    private void scoreChanged(Cmd cmd, int[] score) {
        if (score.length == 2) {
            int score1 = score[0];
            int score2 = score[1];
            model.setScore(score1, score2);
        } else if (score.length == 1) {
            inspectError(cmd, score[0]);
        } else throw new IllegalArgumentException("Incorrect score format");
    }

    private void playerWounded(Cmd cmd, int[] lives) {
        if (lives.length == 2) {
            int myLives = lives[0];
            int enemyLives = lives[1];
            model.setLives(myLives, enemyLives);
        } else if (lives.length == 1) {
            inspectError(cmd, lives[0]);
        } else throw new IllegalArgumentException("Incorrect lives format");
    }

    private void finished(Cmd cmd, int[] data) {
        if (data.length > 1) {
            boolean roundFinished = data[0] == 0; // 0 = finished round, 1 = finished game
            boolean gameFinished = data[0] == 1;
            boolean winner = data[1] > 0;
            if (gameFinished)
                model.gameFinished(winner);
            else if (roundFinished) {
                if (data.length == 4) {
                    int score1 = data[2];
                    int score2 = data[3];
                    model.roundFinished(winner, score1, score2);
                } else throw new IllegalArgumentException("Incorrect finished round format");
            } else inspectError(cmd, data[0]);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect finished format");
    }

    private void thingTaken(Cmd cmd, int[] data) {
        if (data.length == 2) {
            boolean me = data[0] != 0;
            int thingId = data[1];
            if (me)
                model.setThing(thingId);
            else model.setEnemyThing(thingId);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect thing format");
    }

    private void objectAppended(Cmd cmd, int[] data) {
        if (data.length == 3) {
            int id = data[0];
            int objNum = data[1];
            int xy = data[2];
            model.appendObject(objNum, id, xy);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect object format");
    }

    private void checkPromocode(Cmd cmd, int[] data) {
        if (data.length == 1) {
            int res = data[0];
            if (res == 0 || res == 1) {
                model.setPromocodeValid(res == 1);
            } else inspectError(cmd, res);
        } else throw new IllegalArgumentException("Incorrect checkPromocode format");
    }

    private void promocodeDone(Cmd cmd, int[] data) {
        if (data.length > 1) {
            boolean inviter = data[0] == 1;
            int crystals = data[1];
            StringBuilder name = new StringBuilder(); // in Java 8 may be replaced with a StringJoiner
            for (int i = 2; i < data.length; i++) {
                name.append((char) data[i]);
            }
            model.setPromocodeDone(name.toString(), inviter, crystals);
        } else if (data.length == 1) {
            inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect 'promocode done' format");
    }

    private void abilitiesList(Cmd cmd, int[] data) {
        if (data.length > 0) {
            int count = data[0];
            int abilities[] = copyOfRange(data, 1, data.length);
            if (abilities.length == count)
                model.setAbilities(abilities);
            else inspectError(cmd, data[0]);
        } else throw new IllegalArgumentException("Incorrect abilities format");
    }

    private void inspectError(Cmd cmd, int code) {
        switch (code) {
            case 0: // no error
                break;
            case ERR_SIGNIN_INCORRECT_PASSWORD:
                model.setIncorrectCredentials();
                break;
            case ERR_ATTACK_YOURSELF:
                model.setAttackYourself();
                break;
            case ERR_AGGRESSOR_BUSY:
                model.setUserBusy(true);
                break;
            case ERR_DEFENDER_BUSY:
                model.setUserBusy(false);
                break;
            case ERR_BATTLE_NOT_FOUND: // reconnected in a battle screen when the battle had been already finished
                model.setBattleNotFound();
                break;
            case ERR_SIGN_UP:
                model.setSignUpError();
                break;
            case ERR_SIGNIN_INCORRECT_LOGIN:
                model.setIncorrectCredentials();
                break;
            case ERR_NO_CRYSTALS:
                model.setNoCrystals();
                break;
            case ERR_ADD_FRIEND:
                model.setAddFriendError();
                break;
            case ERR_USER_NOT_FOUND:   // incorrect sid, server was restarted, etc.
            case ERR_INCORRECT_TOKEN:  // client was restarted, trying to use the other client, etc.
                model.signIn();
                break;
            case ERR_ENEMY_NOT_FOUND:
                model.setEnemyNotFound();
                break;
            case ERR_NO_FREE_USERS:
                model.setNoFreeUsers();
                break;
            case ERR_INCORRECT_NAME:
                model.setIncorrectName();
                break;
            case ERR_INCORRECT_EMAIL:
                model.setIncorrectEmail();
                break;
            case ERR_DUPLICATE_NAME:
                model.setDuplicateName();
                break;
            default:
                String s = String.format(Locale.getDefault(), "Unhandled error (cmd = %s, code = %d)", cmd, code);
                throw new IllegalArgumentException(s);
        }
    }
}
