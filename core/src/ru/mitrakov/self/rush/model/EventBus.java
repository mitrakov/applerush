package ru.mitrakov.self.rush.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.mitrakov.self.rush.model.Cells.CellObject;

/**
 * Event Bus
 * @author Mitrakov
 */
public class EventBus {
    /** Base event class */
    public static abstract class Event {}

    /** Event: MOVE acknowledge from the Server */
    public static final class MoveResponseEvent extends Event {}
    /** Event: aggressor is busy */
    public static final class AggressorBusyEvent extends Event {}
    /** Event: defender is busy */
    public static final class DefenderBusyEvent extends Event {}
    /** Error: enemy not found */
    public static final class EnemyNotFoundEvent extends Event {}
    /** Event: start waiting for enemy in the Quick Battle mode */
    public static final class WaitingForEnemyEvent extends Event {}
    /** Error: attack yourself*/
    public static final class AttackedYourselfEvent extends Event {}
    /** Error: cannot add a friend */
    public static final class AddFriendErrorEvent extends Event {}
    /** Error: no enough gems to perform an operation */
    public static final class NoCrystalsEvent extends Event {}
    /** Error: incorrect login/password pair */
    public static final class IncorrectCredentialsEvent extends Event {}
    /** Error: incorrect name provided */
    public static final class IncorrectNameEvent extends Event {}
    /** Error: incorrect email provided */
    public static final class IncorrectEmailEvent extends Event {}
    /** Error: name already exists */
    public static final class DuplicateNameEvent extends Event {}
    /** Error: sign-up error */
    public static final class SignUpErrorEvent extends Event {}
    /** Warning: Server is going to be restarted (soft-reboot) */
    public static final class ServerGonnaStopEvent extends Event {}
    /** Error: battle is not found or already finished*/
    public static final class BattleNotFoundEvent extends Event {}
    /** Error: password provided is too weak */
    public static final class WeakPasswordEvent extends Event {}
    /** Error: unsupported protocol version (client needs to be updated) */
    public static final class UnsupportedProtocolEvent extends Event {}
    /** Error: client version is lower than the minimum version */
    public static final class VersionNotAllowedEvent extends Event {
        /** Minimum version allowed */
        public String minVersion;
        /**
         * Creates a new VersionNotAllowed Event
         * @param minVersion minimum version supported by the Server
         */
        VersionNotAllowedEvent(String minVersion) {
            this.minVersion = minVersion;
        }
    }
    /** Event: new version is available to download */
    public static final class NewVersionAvailableEvent extends Event {
        /** New version */
        public String newVersion;
        /**
         * Creates a new NewVersionAvailable Event
         * @param newVersion new version ready for downloading
         */
        NewVersionAvailableEvent(String newVersion) {
            this.newVersion = newVersion;
        }
    }
    /** Event: name is changed (by loading data from settings file or by the Server); also the name may be the same */
    public static final class NameChangedEvent extends Event {
        /** Username */
        public String name;
        /**
         * Creates new NameChanged Event
         * @param name username
         */
        NameChangedEvent(String name) {
            this.name = name;
        }
    }
    /** Event: gems balance has been changed */
    public static final class CrystalChangedEvent extends Event {
        /** Gems count */
        public int crystals;
        /**
         * Creates a new CrystalChanged Event
         * @param crystals updated gems balance
         */
        CrystalChangedEvent(int crystals) {
            this.crystals = crystals;
        }
    }
    /** Event: user's abilities have been changed */
    public static final class AbilitiesExpireUpdatedEvent extends Event {
        /** List of abilities */
        public Iterable<Model.Ability> items;
        /**
         * Creates a new AbilitiesExpireUpdated Event
         * @param items updated list of the user's abilities
         */
        AbilitiesExpireUpdatedEvent(Iterable<Model.Ability> items) {
            this.items = items;
        }
    }
    /** Event: friends list has been updated */
    public static final class FriendListUpdatedEvent extends Event {
        /** List of friends */
        public Collection<FriendItem> items;
        /**
         * Creates a new FriendListUpdated Event
         * @param items updated list of user's friends
         */
        FriendListUpdatedEvent(Collection<FriendItem> items) {
            this.items = items;
        }
    }
    /** Event: new friend has been added */
    public static final class FriendAddedEvent extends Event {
        /** New friend's name */
        public FriendItem name;
        /**
         * Creates a new FriendAdded Event
         * @param name new friend's name
         */
        FriendAddedEvent(FriendItem name) {
            this.name = name;
        }
    }
    /** Event: new friend has been removed */
    public static final class FriendRemovedEvent extends Event {
        /** Ex-friend's name */
        public String name;
        /**
         * Creates a new FriendRemoved Event
         * @param name ex-friend's name
         */
        FriendRemovedEvent(String name) {
            this.name = name;
        }
    }
    /** Event: someone invited us to a battle */
    public static final class InviteEvent extends Event {
        /** Enemy name */
        public String enemy;
        /** Enemy Session ID (required by the Server in response) */
        public int enemySid;
        /**
         * Creates a new Invite Event
         * @param enemy enemy name
         * @param enemySid enemy Session ID
         */
        InviteEvent(String enemy, int enemySid) {
            this.enemy = enemy;
            this.enemySid = enemySid;
        }
    }
    /** Event: the enemy rejected our invite for a battle */
    public static final class StopCallRejectedEvent extends Event {
        /** Enemy name, who rejected our invitation */
        public String cowardName;
        /**
         * Creates a new StopCall Rejected Event
         * @param cowardName enemy name, who rejected our invitation
         */
        StopCallRejectedEvent(String cowardName) {
            this.cowardName = cowardName;
        }
    }
    /** Event: we miss someone's call for a battle, and Server wants us to stop ringing */
    public static final class StopCallMissedEvent extends Event {
        /** Aggressor name */
        public String aggressorName;
        /**
         * Creates a new StopCall Missed Event
         * @param aggressorName aggressor name
         */
        StopCallMissedEvent(String aggressorName) {
            this.aggressorName = aggressorName;
        }
    }
    /** Event: we invite someone for a battle, but he/she missed (or ignored) our invitation */
    public static final class StopCallExpiredEvent extends Event {
        /** Defender name */
        public String defenderName;
        /**
         * Creates a new StopCall Expired Event
         * @param defenderName defender name
         */
        StopCallExpiredEvent(String defenderName) {
            this.defenderName = defenderName;
        }
    }
    /** Event: ranking updated */
    @SuppressWarnings("WeakerAccess")
    public static final class RatingUpdatedEvent extends Event {
        /** Rating type (General/Weekly) */
        public Model.RatingType type;
        /** Rating items list */
        public Iterable<RatingItem> items;
        /**
         * Creates a new Rating Updated Event
         * @param type rating type (General/Weekly)
         * @param items updated list of rating items for the given rating type
         */
        RatingUpdatedEvent(Model.RatingType type, Iterable<RatingItem> items) {
            this.type = type;
            this.items = items;
        }
    }
    /** Event: round has been finished */
    public static final class RoundFinishedEvent extends Event {
        /** Winner flag (TRUE if we won) */
        public boolean winner;
        /** Participant #1 */
        public String detractor1;
        /** Participant #2 */
        public String detractor2;
        /** Total score1 (for the whole game) */
        public int totalScore1;
        /** Total score2 (for the whole game) */
        public int totalScore2;
        /**
         * Creates a new Round Finished Event
         * @param winner winner flag (TRUE if we won)
         * @param detractor1 participant #1
         * @param detractor2 participant #2
         * @param totalScore1 total score1
         * @param totalScore2 total score2
         */
        RoundFinishedEvent(boolean winner, String detractor1, String detractor2, int totalScore1, int totalScore2) {
            this.winner = winner;
            this.detractor1 = detractor1;
            this.detractor2 = detractor2;
            this.totalScore1 = totalScore1;
            this.totalScore2 = totalScore2;
        }
    }
    public static final class GameFinishedEvent extends Event {
        public boolean winner;
        public String detractor1;
        public String detractor2;
        public int totalScore1;
        public int totalScore2;
        public int reward;
        GameFinishedEvent(boolean winner, String d1, String d2, int totalScore1, int totalScore2, int reward) {
            this.winner = winner;
            this.detractor1 = d1;
            this.detractor2 = d2;
            this.totalScore1 = totalScore1;
            this.totalScore2 = totalScore2;
            this.reward = reward;
        }
    }
    public static final class PromocodeDoneEvent extends Event {
        public String name;
        public boolean inviter;
        public int crystals;
        PromocodeDoneEvent(String name, boolean inviter, int crystals) {
            this.name = name;
            this.inviter = inviter;
            this.crystals = crystals;
        }
    }
    public static final class StyleChangedEvent extends Event {
        public int stylePack;
        StyleChangedEvent(int stylePack) {
            this.stylePack = stylePack;
        }
    }
    public static final class CharacterChangedEvent extends Event {
        public Model.Character character;
        CharacterChangedEvent(Model.Character character) {
            this.character = character;
        }
    }
    public static final class AbilitiesChangedEvent extends Event {
        public Iterable<Model.Ability> items;
        AbilitiesChangedEvent(Iterable<Model.Ability> items) {
            this.items = items;
        }
    }
    public static final class EnemyNameChangedEvent extends Event {
        public String enemy;
        EnemyNameChangedEvent(String enemy) {
            this.enemy = enemy;
        }
    }
    public static final class RoundStartedEvent extends Event {
        public int number;
        public String levelName;
        RoundStartedEvent(int number, String levelName) {
            this.number = number;
            this.levelName = levelName;
        }
    }
    public static final class NewFieldEvent extends Event {
        public CellObject actor;
        public Field field;
        NewFieldEvent(CellObject actor, Field field) {
            this.actor = actor;
            this.field = field;
        }
    }
    public static final class ScoreChangedEvent extends Event {
        public int score1;
        public int score2;
        ScoreChangedEvent(int score1, int score2) {
            this.score1 = score1;
            this.score2 = score2;
        }
    }
    @SuppressWarnings("WeakerAccess")
    public static final class LivesChangedEvent extends Event {
        public int myLives;
        public int enemyLives;
        LivesChangedEvent(int myLives, int enemyLives) {
            this.myLives = myLives;
            this.enemyLives = enemyLives;
        }
    }
    @SuppressWarnings("WeakerAccess")
    public static final class PlayerWoundedEvent extends Event {
        public int xy;
        public Model.HurtCause cause;
        public int myLives;
        public int enemyLives;
        PlayerWoundedEvent(int xy, Model.HurtCause cause, int myLives, int enemyLives) {
            this.xy = xy;
            this.cause = cause;
            this.myLives = myLives;
            this.enemyLives = enemyLives;
        }
    }
    public static final class EffectAddedEvent extends Event {
        public Model.Effect effect;
        EffectAddedEvent(Model.Effect effect) {
            this.effect = effect;
        }
    }
    public static final class ObjectRemovedEvent extends Event {
        public int oldXy;
        public CellObject obj;
        ObjectRemovedEvent(int oldXy, CellObject obj) {
            this.oldXy = oldXy;
            this.obj = obj;
        }
    }
    public static final class ActorResetEvent extends Event {
        public CellObject obj;
        ActorResetEvent(CellObject obj) {
            this.obj = obj;
        }
    }
    public static final class ThingChangedEvent extends Event {
        public CellObject oldThing;
        public CellObject newThing;
        public boolean mine;
        ThingChangedEvent(CellObject oldThing, CellObject newThing, boolean mine) {
            this.oldThing = oldThing;
            this.newThing = newThing;
            this.mine = mine;
        }
    }
    public static final class AuthorizedChangedEvent extends Event {
        public boolean authorized;
        AuthorizedChangedEvent(boolean authorized) {
            this.authorized = authorized;
        }
    }
    public static final class ConnectedChangeEvent extends Event {
        public boolean connected;
        ConnectedChangeEvent(boolean connected) {
            this.connected = connected;
        }
    }
    public static final class PromocodeValidChangedEvent extends Event {
        public boolean valid;
        PromocodeValidChangedEvent(boolean valid) {
            this.valid = valid;
        }
    }
    public static final class SkuGemsUpdatedEvent extends Event {
        public Map<String, Integer> skuGems;
        SkuGemsUpdatedEvent(Map<String, Integer> skuGems) {
            this.skuGems = skuGems;
        }
    }
    public static final class PaymentDoneEvent extends Event {
        public int gems;
        public String coupon;
        PaymentDoneEvent(int gems, String coupon) {
            this.gems = gems;
            this.coupon = coupon;
        }
    }


    // =====================================
    // =====================================


    public interface Listener {
        void OnEvent(Event event);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    public void addListener(Listener listener) {
        assert listener != null;
        listeners.add(listener);
    }

    void raise(Event event) {
        assert event != null;
        for (int i = 0; i < listeners.size(); i++) { // do NOT use iterators! They produces excessive work for GC
            Listener listener = listeners.get(i); assert listener != null;
            listener.OnEvent(event);
        }
    }
}
