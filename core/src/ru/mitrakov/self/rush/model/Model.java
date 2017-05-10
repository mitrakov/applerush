package ru.mitrakov.self.rush.model;

import java.util.*;
import java.security.*;
import java.math.BigInteger;
import java.util.concurrent.*;

import ru.mitrakov.self.rush.model.object.CellObject;

import static ru.mitrakov.self.rush.utils.SimpleLogger.*;
import static ru.mitrakov.self.rush.utils.Utils.*;
import static ru.mitrakov.self.rush.model.Model.Cmd.*;

/**
 * This class represents a model in the MVC pattern
 * Class is intended to have a single instance
 *
 * @author mitrakov
 */
@SuppressWarnings("WeakerAccess")
public class Model {
    /**
     * size of the rating list (defined by server)
     */
    public static final int RATINGS_COUNT = 10;
    public static final int STYLES_COUNT = 4;
    public static final int HISTORY_MAX = 32;

    // ===========================
    // === PUBLIC INTERFACES ===
    // ===========================

    /**
     * interface to send commands to the server
     */
    public interface ISender {
        void send(Cmd cmd);

        void send(Cmd cmd, int arg);

        void send(Cmd cmd, byte[] data);

        void reset();
    }

    /**
     * interface to read/write files independent from a platform (Desktop, Android, etc.)
     */
    public interface IFileReader {
        void write(String filename, String s);

        void append(String filename, String s);

        String read(String filename);

        Object deserialize(String filename);

        void serialize(String filename, Object obj);
    }

    // ===========================
    // === PUBLIC ENUMERATIONS ===
    // ===========================

    /**
     * server-specific commands; for more details see docs to the protocol
     */
    public enum Cmd {
        UNSPEC_ERROR, SIGN_UP, SIGN_IN, SIGN_OUT, USER_INFO, ATTACK, CALL, ACCEPT, REJECT, STOPCALL, CANCEL_CALL,
        RANGE_OF_PRODUCTS, BUY_PRODUCT, RECEIVE_TRAINING, CHANGE_CHARACTER, RESERVED_0F, FULL_STATE, ABILITY_LIST,
        MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN, USE_THING, USE_SKILL, STATE_CHANGED, SCORE_CHANGED, PLAYER_WOUNDED,
        THING_TAKEN, OBJECT_APPENDED, FINISHED, GIVE_UP, ROUND_INFO, RATING, FRIEND_LIST, ADD_FRIEND, REMOVE_FRIEND,
        CHECK_PROMOCODE, PROMOCODE_DONE
    }

    public enum Character {None, Rabbit, Hedgehog, Squirrel, Cat}

    /**
     * ability list; some abilities are stubs (a7, a8, up to a32), because skills start with an index=33
     */
    public enum Ability {
        None, Snorkel, Shoes, SouthWester, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16,
        a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, Sapper
    }

    /**
     * rating enumeration (General, Weekly, etc.); constants (0, 1) are specified by the server
     */
    public enum RatingType {
        General, Weekly
    }

    // =============================
    // === PUBLIC STATIC METHODS ===
    // =============================

    public static synchronized String md5(String s) {
        log("calculating md5 for: " + s);
        try {
            // @mitrakov: don't use HexBinaryAdapter(): javax is not supported by Android
            byte[] bytes = MessageDigest.getInstance("md5").digest(getBytes(s));
            String res = String.format("%032x", new BigInteger(1, bytes)); // use "%032x" instead of "%32x"!
            log("md5 = " + res);
            return res;
        } catch (NoSuchAlgorithmException ignored) {
            return "";
        }
    }

    // ==============================
    // === PUBLIC VOLATILE FIELDS ===
    // ==============================
    // getters are supposed to have a little overhead, so we make the fields "public" for efficiency and "volatile" for
    // memory management inside a multithreading access; be careful! They may be changed OUTSIDE the OpenGL loop
    // ==============================

    public volatile String name = "";
    public volatile String enemy = "";
    public volatile String promocode = "";
    public volatile Character character = Character.None;
    public volatile Character character1 = Character.None;
    public volatile Character character2 = Character.None;
    public volatile boolean connected = true;
    public volatile boolean authorized = false;
    public volatile boolean promocodeValid = false;
    public volatile boolean newbie = true;
    public volatile int crystals = 0;
    public volatile int totalScore1 = 0;
    public volatile int totalScore2 = 0;
    public volatile int roundNumber = 0;
    public volatile int roundLengthSec = 60;
    public volatile int stylePack = 0;
    public volatile long abilityExpireTime = 0;
    public volatile long roundStartTime = 0;
    public volatile Field field;
    public volatile CellObject curActor;

    // ==================================================
    // === PUBLIC NON-VOLATILE CONCURRENT COLLECTIONS ===
    // ==================================================
    // getters are supposed to have a little overhead, so we make the fields "public" for efficiency; these collections
    // are rest upon Java-Concurrent Library, because they may be changed OUTSIDE the OpenGL loop at any moment;
    // all 'foreach' operations are considered to be safe
    // ==================================================

    public final Collection<Product> products = new ConcurrentLinkedQueue<Product>();
    public final Collection<HistoryItem> history = new ConcurrentLinkedQueue<HistoryItem>();
    public final Collection<FriendItem> friends = new ConcurrentLinkedQueue<FriendItem>();
    public final Map<Ability, Integer> abilityExpireMap = new ConcurrentHashMap<Ability, Integer>(); // see note#1

    // ===========================
    // === PUBLIC FINAL FIELDS ===
    // ===========================

    public final EventBus bus = new EventBus();

    // ================
    // === SETTINGS ===
    // ================

    public volatile boolean notifyNewBattles = true;
    public volatile boolean languageEn = true; // convert to enum in the future

    // ================================
    // === PRIVATE STATIC CONSTANTS ===
    // ================================

    private static final int AGGRESSOR_ID = 1;
    private static final int DEFENDER_ID = 2;
    private static final int PING_PERIOD_MSEC = 60000;
    private static final int SKILL_OFFSET = 0x20;
    private static final int PROMOCODE_LEN = 5;
    private static final String SETTINGS_FILE = "settings";
    private static final String HISTORY_PREFIX = "history/";

    // ============================
    // === USUAL PRIVATE FIELDS ===
    // ============================

    private final Object locker = new Object();
    private final Collection<Ability> abilities = new LinkedList<Ability>();
    private ISender sender;
    private IFileReader fileReader;
    private String hash = "";
    private boolean aggressor = true;
    private CellObject curThing;
    private CellObject enemyThing;

    public Model() {
        // create timer to ping the server (otherwise the server will make "signOut due to inaction")
        new Timer("Ping timer", true).schedule(new TimerTask() {
            @Override
            public void run() {
                if (authorized)
                    getUserInfo();
            }
        }, PING_PERIOD_MSEC, PING_PERIOD_MSEC);
    }

    // ==========================
    // === NON-SERVER METHODS ===
    // ==========================

    /**
     * Sets a new sender to the model
     *
     * @param sender - sender (may be NULL)
     */
    public void setSender(ISender sender) {
        this.sender = sender;
    }

    /**
     * Sets a new file reader to the model
     *
     * @param fileReader - file reader (may be NULL)
     */
    public void setFileReader(IFileReader fileReader) {
        this.fileReader = fileReader;
    }

    /**
     * Loads settings from the internal file
     */
    public void loadSettings() {
        if (fileReader != null) {
            String s = fileReader.read(SETTINGS_FILE);
            if (s != null) {
                String[] settings = s.split(" ");
                if (settings.length > 2) {
                    languageEn = settings[0].equals("e");
                    notifyNewBattles = settings[1].equals("1");
                    name = settings[2];
                    if (settings.length > 3)
                        hash = settings[3];
                }
                newbie = false;
            }
        }
    }

    /**
     * Saves current settings to the internal file
     */
    public void saveSettings() {
        if (fileReader != null) {
            String s = String.format("%s %s %s %s", languageEn ? "e" : "r", notifyNewBattles ? "1" : "0", name, hash);
            fileReader.write(SETTINGS_FILE, s);
        }
    }

    /**
     * @param ability - ability (if NULL then returns empty list)
     * @return collection of available products by the given ability (e.g. Snorkel for 1 day, 3 days, 7 days)
     */
    public Collection<Product> getProductsByAbility(Ability ability) {
        Collection<Product> res = new LinkedList<Product>();
        for (Product product : products) {   // in Java 8 may be replaced with lambda
            if (product.ability == ability)
                res.add(product);
        }
        return res;
    }

    public boolean friendExists(String name) {
        for (FriendItem item : friends) { // in Java 8 may be replaced with lambda
            if (item.name.equals(name))
                return true;
        }
        return false;
    }

    // ==============================
    // === SERVER REQUEST METHODS ===
    // ==============================
    // Feel free to call these methods from anywhere
    // ==============================

    public void signIn() {
        assert name != null && hash != null;
        if (name.length() > 0 && hash.length() > 0 && connected && sender != null) { // don't use method 'isEmpty()'
            sender.reset();
            sender.send(SIGN_IN, getBytes(String.format("\1%s\0%s", name, hash))); // \1 = Local auth
        }
    }

    /**
     * Sends SIGN_IN command to the server
     *
     * @param login    - user name
     * @param password - password
     */
    public void signIn(String login, String password) {
        if (connected && sender != null) {
            hash = md5(password);
            sender.reset();
            sender.send(SIGN_IN, getBytes(String.format("\1%s\0%s", login, hash))); // \1 = Local auth
        }
    }

    /**
     * Sends SIGN_UP command to the server
     *
     * @param login    - user name
     * @param password - password
     * @param email    - email address
     */
    public void signUp(String login, String password, String email, String promocode) {
        assert login != null && password != null && email != null && promocode != null;
        if (connected && sender != null && password.length() >= 4) {
            hash = md5(password);
            sender.reset();
            sender.send(SIGN_UP, getBytes(String.format("%s\0%s\0%s\0%s", login, hash, email, promocode)));
        }
    }

    /**
     * Sends SIGN_OUT command to the server
     */
    public void signOut() {
        if (connected && sender != null) {
            sender.send(SIGN_OUT);
        }
    }

    public void getUserInfo() {
        if (connected && sender != null) {
            sender.send(USER_INFO);
        }
    }

    /**
     * Sends INVITE command to the server (by name)
     *
     * @param victim - victim user name
     */
    public void invite(String victim) {
        if (connected && sender != null) {
            sender.send(ATTACK, getBytes(String.format("\0%s", victim)));
        }
    }

    /**
     * Sends INVITE command to the server (latest enemy)
     */
    public void inviteLatest() {
        if (connected && sender != null) {
            sender.send(ATTACK, 1);
        }
    }

    /**
     * Sends INVITE command to the server (random enemy)
     */
    public void inviteRandom() {
        if (connected && sender != null) {
            sender.send(ATTACK, 2);
        }
    }

    /**
     * Sends ACCEPT command to the server (in response to INVITE)
     */
    public void accept(int enemySid) {
        if (connected && sender != null) {
            sender.send(ACCEPT, new byte[]{(byte) (enemySid / 256), (byte) (enemySid % 256)});
        }
    }

    /**
     * Sends REJECT command to the server (in response to INVITE)
     */
    public void reject(int enemySid) {
        if (connected && sender != null) {
            sender.send(REJECT, new byte[]{(byte) (enemySid / 256), (byte) (enemySid % 256)});
        }
    }

    public void receiveTraining() {
        if (connected && sender != null) {
            sender.send(RECEIVE_TRAINING);
        }
    }

    public void changeCharacter(Character character) {
        if (character != Character.None) {
            if (connected && sender != null) {
                sender.send(CHANGE_CHARACTER, character.ordinal());
            }
        }
    }

    /**
     * Sends FRIEND_LIST command to the server
     */
    public void getFriends() {
        if (connected && sender != null) {
            sender.send(FRIEND_LIST);
        }
    }

    /**
     * Sends ADD_FRIEND command to the server
     *
     * @param name - friend user name
     */
    public void addFriend(String name) {
        if (connected && sender != null) {
            if (name.length() > 0)
                sender.send(ADD_FRIEND, getBytes(name));
        }
    }

    /**
     * Sends REMOVE_FRIEND command to the server
     *
     * @param name - quondam friend name
     */
    public void removeFriend(String name) {
        if (connected && sender != null) {
            if (name.length() > 0)
                sender.send(REMOVE_FRIEND, getBytes(name));
        }
    }

    /**
     * Sends RATING command to the server
     *
     * @param type - type of rating (General, Weekly, etc.)
     */
    public void getRating(RatingType type) {
        assert type != null;
        if (connected && sender != null) {
            sender.send(RATING, type.ordinal());
        }
    }

    public void checkPromocode(String promocode) {
        assert promocode != null;
        if (connected && sender != null && promocode.length() >= PROMOCODE_LEN) {
            sender.send(CHECK_PROMOCODE, getBytes(promocode));
        }
    }

    /**
     * Sends BUY_PRODUCT command to the server
     *
     * @param product - product to buy
     */
    public void buyProduct(Product product) {
        assert product != null;
        if (connected && sender != null) {
            sender.send(BUY_PRODUCT, new byte[]{(byte) product.ability.ordinal(), (byte) product.days});
        }
    }

    public void cancelCall() {
        if (connected && sender != null) {
            sender.send(CANCEL_CALL);
        }
    }

    /**
     * Sends MOVE_LEFT battle command to the server
     */
    public void moveLeft() {
        if (connected && sender != null && curActor != null) {
            if (curActor.getX() > 0)
                sender.send(MOVE_LEFT);
        }
    }

    /**
     * Sends MOVE_RIGHT battle command to the server
     */
    public void moveRight() {
        if (connected && sender != null && curActor != null) {
            if (curActor.getX() < Field.WIDTH - 1)
                sender.send(MOVE_RIGHT);
        }
    }

    /**
     * Sends MOVE_UP battle command to the server
     */
    public void moveUp() {
        if (connected && sender != null && curActor != null) {
            if (curActor.getY() > 0)
                sender.send(MOVE_UP);
        }
    }

    /**
     * Sends MOVE_DOWN battle command to the server
     */
    public void moveDown() {
        if (connected && sender != null && curActor != null) {
            if (curActor.getY() < Field.HEIGHT - 1)
                sender.send(MOVE_DOWN);
        }
    }

    /**
     * Sends USE_THING battle command to the server
     */
    public void useThing() {
        if (connected && sender != null && curThing != null) {
            sender.send(USE_THING, curThing.getId());
        }
    }

    /**
     * Sends USE_SKILL battle command to the server
     *
     * @param ability - ability to use (it must be a SKILL, i.e. has a number > SKILL_OFFSET)
     */
    public void useAbility(Ability ability) {
        assert ability != null;
        if (connected && sender != null) {
            if (ability.ordinal() > SKILL_OFFSET) // only skills may be used
                sender.send(USE_SKILL, ability.ordinal());
        }
    }

    public void useAbility(int index) {
        int i = 0;
        for (Ability ability : abilities) {
            if (i++ == index)
                useAbility(ability);
        }
    }

    public void giveUp() {
        field = null; // reset the current field
        if (connected && sender != null) {
            sender.send(GIVE_UP);
        }
    }

    // ===============================
    // === SERVER RESPONSE METHODS ===
    // ===============================
    // These methods are not expected to be called from external code
    // ===============================

    public void setConnected(boolean value) {
        if (!connected && value) { // if changed "not_connected" -> "connected"
            connected = true;
            if (authorized)
                getUserInfo(); // connected, but already authorized? possibly the server has been restarted: see note#4
            else signIn();     // connected and not authorized: try to sign in using stored credentials
        }
        connected = value;
    }

    public void setAuthorized(boolean value) {
        authorized = value;
        if (connected && sender != null) {
            if (authorized) {
                sender.send(RANGE_OF_PRODUCTS);
                sender.send(FRIEND_LIST); // without this "InviteByName" dialog suggests to add everyone to friends
            } else {
                hash = "";
                saveSettings(); // to write empty hash to a local storage
            }
        }
    }

    public void setUserInfo(int[] data) {
        assert data != null;
        int i = 0;

        // parse name
        StringBuilder bld = new StringBuilder();
        for (; i < data.length && data[i] != 0; i++) {
            bld.append((char) data[i]);
        }
        name = bld.toString();
        i++;

        // parse promo code
        bld = new StringBuilder(name);
        for (; i < data.length && data[i] != 0; i++) {
            bld.append((char) data[i]);
        }
        promocode = bld.toString();
        i++;

        // parse character
        Character[] characters = Character.values();
        int ch = data[i++];
        if (0 <= ch && ch < characters.length && character != characters[ch]) {
            character = characters[ch];
            bus.raise(new EventBus.CharacterChangedEvent(character));
        }

        // parse crystals
        if (i + 3 < data.length)
            crystals = (data[i] << 24) | (data[i + 1] << 16) | (data[i + 2] << 8) | (data[i + 3]); // what if > 2*10^9?
        i += 4;

        // parse abilities
        Ability[] array = Ability.values();
        synchronized (locker) {
            abilityExpireMap.clear();
            int abilitiesCnt = data[i++];
            for (int j = 0; j < abilitiesCnt; j++, i += 3) {
                if (i + 2 < data.length) {
                    int id = data[i];
                    int minutes = data[i + 1] * 256 + data[i + 2];
                    if (0 <= id && id < array.length)
                        abilityExpireMap.put(array[id], minutes);
                }
            }
        }
        abilityExpireTime = System.currentTimeMillis();
        // fire the event (we use TreeSet to implicitly sort the key set; of course we may use ConcurrentSkipListMap
        // for "abilityExpireMap", but it's not supported by API Level 8, so we use ConcurrentHashMap)
        bus.raise(new EventBus.AbilitiesExpireUpdatedEvent(new TreeSet<Ability>(abilityExpireMap.keySet())));

        // now we know valid user name => read the history from a local storage
        if (fileReader != null && history.isEmpty()) {
            Object lst = fileReader.deserialize(String.format("%s%s", HISTORY_PREFIX, name));
            if (lst instanceof Collection) { // stackoverflow.com/questions/2950319
                //noinspection unchecked
                history.addAll((Collection) lst);
            }
        }

        // now we know valid user name => save settings
        saveSettings();
    }

    public void setVictim(String victimName) {
        enemy = victimName;
    }

    public void attacked(int sid, String aggressorName) {
        enemy = aggressorName;
        bus.raise(new EventBus.InviteEvent(aggressorName, sid));
    }

    public void stopCallRejected(String coward) {
        bus.raise(new EventBus.StopCallRejectedEvent(coward));
    }

    public void stopCallMissed(String aggressorName) {
        bus.raise(new EventBus.StopCallMissedEvent(aggressorName));
    }

    public void stopCallExpired(String defenderName) {
        bus.raise(new EventBus.StopCallExpiredEvent(defenderName));
    }

    public void setFriendList(int[] data) {
        assert data != null;
        Character[] characters = Character.values();

        synchronized (locker) {
            friends.clear();
            String s = newString(toByte(data, data.length));  // example: \3Tommy\0\2Bobby\0\3Billy\0
            if (s.length() > 0) { // be careful! if s == "", then s.split("\0") returns Array("") instead of Array()
                for (String item : s.split("\0")) {
                    byte ch = (byte) item.charAt(0);
                    if (0 <= ch && ch < characters.length) {
                        friends.add(new FriendItem(characters[ch], item.substring(1)));
                    }
                }
            }
        }
        bus.raise(new EventBus.FriendListUpdatedEvent(friends));
    }

    public void friendAdded(int character, String name) {
        Character[] characters = Character.values();
        if (0 <= character && character < characters.length) {
            FriendItem item = new FriendItem(characters[character], name);
            friends.add(item);
            bus.raise(new EventBus.FriendAddedEvent(item));
        }
    }

    public void friendRemoved(String name) {
        synchronized (locker) {
            for (FriendItem item : friends) { // in Java 8 may be replaced with lambda
                if (item.name.equals(name)) {
                    friends.remove(item);
                    break; // to avoid iterator exceptions
                }
            }
        }
        bus.raise(new EventBus.FriendRemovedEvent(name));
    }

    public void setRangeOfProducts(final int[] data) {
        assert data != null;
        Ability[] abs = Ability.values();
        synchronized (locker) {
            products.clear();
            for (int i = 0; i + 2 < data.length; i += 3) {
                int id = data[i];
                int days = data[i + 1];
                int cost = data[i + 2];
                if (0 <= id && id < abs.length)
                    products.add(new Product(abs[id], days, cost));
            }
        }
    }

    public void setRating(RatingType type, int[] data) {
        assert type != null && data != null;
        Collection<RatingItem> rating = new LinkedList<RatingItem>();

        int i = 0;
        while (i < data.length) {
            // name
            StringBuilder name = new StringBuilder();
            int wins = 0, losses = 0, score_diff = 0;
            for (; data[i] != 0 && i < data.length; i++) {
                name.append((char) data[i]);
            }
            i++;
            // wins
            if (i + 3 < data.length) {
                wins = (data[i] << 24) | (data[i + 1] << 16) | (data[i + 2] << 8) | (data[i + 3]); // if > 2*10^9?
                i += 4;
            }
            // losses
            if (i + 3 < data.length) {
                losses = (data[i] << 24) | (data[i + 1] << 16) | (data[i + 2] << 8) | (data[i + 3]); // if > 2*10^9?
                i += 4;
            }
            // score_diff
            if (i + 3 < data.length) {
                score_diff = (data[i] << 24) | (data[i + 1] << 16) | (data[i + 2] << 8) | (data[i + 3]); // if > 2*10^9?
                i += 4;
            }
            rating.add(new RatingItem(name.toString(), wins, losses, score_diff));
        }

        bus.raise(new EventBus.RatingUpdatedEvent(type, rating));
    }

    public void setPromocodeValid(boolean valid) {
        promocodeValid = valid;
    }

    public void setPromocodeDone(String name, boolean inviter, int crystals) {
        assert name != null;
        bus.raise(new EventBus.PromocodeDoneEvent(name, inviter, crystals));
    }

    public void setRoundInfo(int number, int timeSec, boolean aggressor, int character1, int character2, int myLives,
                             int enemyLives) {
        curThing = enemyThing = curActor = null;

        Character[] characters = Character.values();
        if (0 <= character1 && character1 < characters.length)
            this.character1 = characters[character1];
        if (0 <= character2 && character2 < characters.length)
            this.character2 = characters[character2];

        roundNumber = number;
        roundLengthSec = timeSec;
        this.aggressor = aggressor;
        roundStartTime = System.currentTimeMillis();
        bus.raise(new EventBus.ScoreChangedEvent(0, 0));
        bus.raise(new EventBus.LivesChangedEvent(myLives, enemyLives, true));
    }

    public void setNewField(int[] fieldData) {
        Field field; // for multithreaded safety
        this.field = field = new Field(fieldData);
        // assign curActor (be careful! if "fieldData" doesn't contain actors, curActor will become NULL! it may be
        // assigned later in appendObject() method)
        curActor = aggressor ? field.getObject(AGGRESSOR_ID) : field.getObject(DEFENDER_ID);
    }

    public void appendObject(int number, int id, int xy) {
        Field field;
        synchronized (locker) {
            field = this.field;
        }
        if (field != null) {
            field.appendObject(number, id, xy);
            if (id == AGGRESSOR_ID || id == DEFENDER_ID)
                curActor = aggressor ? field.getObject(AGGRESSOR_ID) : field.getObject(DEFENDER_ID);
        }
    }

    public void setStylePack(int pack) {
        stylePack = pack;
        bus.raise(new EventBus.StyleChangedEvent(pack));
    }

    public void setXy(int number, int xy) {
        Field field;
        synchronized (locker) {
            field = this.field;
        }
        if (field != null)
            field.setXy(number, xy);
    }

    public void setScore(int score1, int score2) {
        bus.raise(new EventBus.ScoreChangedEvent(score1, score2));
    }

    public void setThing(int thingId) {
        CellObject oldThing = curThing;
        curThing = Cell.newObject(thingId, 0xFF, new Field.NextNumber() {
            @Override
            public int next() {
                return 0;
            }
        });
        bus.raise(new EventBus.ThingChangedEvent(oldThing, curThing, true));
    }

    public void setEnemyThing(int thingId) {
        CellObject oldThing = enemyThing;
        enemyThing = Cell.newObject(thingId, 0xFF, new Field.NextNumber() {
            @Override
            public int next() {
                return 0;
            }
        });
        bus.raise(new EventBus.ThingChangedEvent(oldThing, enemyThing, false));
    }

    public void setLives(int myLives, int enemyLives) {
        bus.raise(new EventBus.LivesChangedEvent(myLives, enemyLives, false));
    }

    public void roundFinished(boolean winner, int totalScore1, int totalScore2) {
        this.totalScore1 = totalScore1;
        this.totalScore2 = totalScore2;
        bus.raise(new EventBus.RoundFinishedEvent(winner));
    }

    public void gameFinished(boolean winner) {
        // updating history
        if (enemy.length() > 0) { // it may be empty, e.g. in the Training Level
            // building a history item
            HistoryItem item = new HistoryItem(new Date(), winner, aggressor ? name : enemy, aggressor ? enemy : name,
                    character1, character2, totalScore1, totalScore2);

            // prepend the item into the current history (and delete old items if necessary)
            List<HistoryItem> lst = new LinkedList<HistoryItem>(history);
            lst.add(0, item);
            while (lst.size() > HISTORY_MAX)
                lst.remove(HISTORY_MAX);
            history.clear();
            history.addAll(lst);

            // store the current history in the local storage
            if (fileReader != null)
                fileReader.serialize(String.format("%s%s", HISTORY_PREFIX, name), lst);
        }

        // reset reference to a field
        field = null;
        bus.raise(new EventBus.GameFinishedEvent(winner));
    }

    public void setAbilities(int[] ids) {
        assert ids != null;
        synchronized (locker) {
            abilities.clear();
            Ability[] array = Ability.values();
            for (int id : ids) {
                if (0 <= id && id < array.length)
                    abilities.add(array[id]);
            }
        }
        bus.raise(new EventBus.AbilitiesChangedEvent(abilities));
    }

    public void setUserBusy(boolean aggressor) {
        bus.raise(aggressor ? new EventBus.AggressorBusyEvent() : new EventBus.DefenderBusyEvent());
    }

    public void setEnemyNotFound() {
        bus.raise(new EventBus.EnemyNotFoundEvent());
    }

    public void setNoFreeUsers() {
        bus.raise(new EventBus.NoFreeUsersEvent());
    }

    public void setAttackYourself() {
        bus.raise(new EventBus.AttackedYourselfEvent());
    }

    public void setAddFriendError() {
        bus.raise(new EventBus.AddFriendErrorEvent());
    }

    public void setNoCrystals() {
        bus.raise(new EventBus.NoCrystalsEvent());
    }

    public void setIncorrectCredentials() {
        bus.raise(new EventBus.IncorrectCredentialsEvent());
    }

    public void setIncorrectName() {
        bus.raise(new EventBus.IncorrectNameEvent());
    }

    public void setIncorrectEmail() {
        bus.raise(new EventBus.IncorrectEmailEvent());
    }

    public void setDuplicateName() {
        bus.raise(new EventBus.DuplicateNameEvent());
    }

    public void setSignUpError() {
        bus.raise(new EventBus.SignUpErrorEvent());
    }

    public void setBattleNotFound() {
        bus.raise(new EventBus.BattleNotFoundEvent());
        field = null; // reset reference to a field
    }
}

// note#2 (@mitrakov, 2017-04-03): it'd be better use SkipListMap, but it's not supported by Android API 8
//
// note#4 (@mitrakov, 2017-04-21): suppose the server has been suddenly restarted; a user may request smth (e.g. Random
// Opponent); server won't respond and the "Connecting" dialog will be shown; then after re-connecting a user may retry
// its request (Random Opponent), but the server will return "NO_USER_FOUND" so that a client will need to re-sign in.
// it means that the request will also fail; to resolve this problem here we send a fake request (e.g. USER_INFO)
// intentionally to obtain "NO_USER_FOUND" and afterwards re-sign in.
// Someone may ask "what if send SIGN_IN right away?" It's a mistake because client might just been re-connected
// without the server being restarted, so that requesting SIGN_IN would be erroneous
