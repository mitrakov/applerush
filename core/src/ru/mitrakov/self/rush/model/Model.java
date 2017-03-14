package ru.mitrakov.self.rush.model;

import java.util.*;
import java.util.concurrent.*;
import java.text.DateFormat;

import ru.mitrakov.self.rush.model.object.CellObject;

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

    /**
     * interface to send commands to the server
     */
    public interface ISender {
        void send(Cmd cmd);

        void send(Cmd cmd, int arg);

        void send(Cmd cmd, byte[] data);

        void resetSid();
    }

    /**
     * interface to read/write files independent from a platform (Desktop, Android, etc.)
     */
    public interface IFileReader {
        void write(String filename, String s);

        String read(String filename);
    }

    /**
     * server-specific commands; for more details see docs to the protocol
     */
    public enum Cmd {
        UNSPEC_ERROR, SIGN_UP, SIGN_IN, SIGN_OUT, USER_INFO, ATTACK, INVITE, ACCEPT, REJECT, REFUSED, RANGE_OF_PRODUCTS,
        BUY_PRODUCT, RATING, FRIEND_LIST, ADD_FRIEND, REMOVE_FRIEND, FULL_STATE, ABILITY_LIST, MOVE_LEFT, MOVE_RIGHT,
        MOVE_UP, MOVE_DOWN, USE_THING, USE_SKILL, STATE_CHANGED, SCORE_CHANGED, PLAYER_WOUNDED, THING_TAKEN,
        OBJECT_APPENDED, FINISHED
    }

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

    // ==============================
    // === PUBLIC VOLATILE FIELDS ===
    // ==============================
    // getters are supposed to have a little overhead, so we make the fields "public" for efficiency and "volatile" for
    // memory management inside a multithreading access; be careful! They may be changed OUTSIDE the OpenGL loop
    // ==============================

    public volatile String name = "";
    public volatile String enemy = "";
    public volatile boolean authorized = false;
    public volatile boolean roundWinner = false;
    public volatile int crystals = 0;
    public volatile int score1 = 0;
    public volatile int score2 = 0;
    public volatile int totalScore1 = 0;
    public volatile int totalScore2 = 0;
    public volatile long generalRatingTime = 0;
    public volatile long weeklyRatingTime = 0;
    public volatile long inviteTime = 0;
    public volatile long refusedRejectedTime = 0;
    public volatile long refusedMissedTime = 0;
    public volatile long roundFinishedTime = 0;
    public volatile long gameFinishedTime = 0;
    public volatile Field field;
    public volatile CellObject curActor;
    public volatile CellObject curThing;

    // ==================================================
    // === PUBLIC NON-VOLATILE CONCURRENT COLLECTIONS ===
    // ==================================================
    // getters are supposed to have a little overhead, so we make the fields "public" for efficiency; these collections
    // are rest upon Java-Concurrent Library, because they may be changed OUTSIDE the OpenGL loop at any moment;
    // all 'foreach' operations are considered to be safe
    // ==================================================

    public final Collection<Ability> abilities = new ConcurrentLinkedQueue<Ability>(); // ....
    public final Collection<Product> products = new ConcurrentLinkedQueue<Product>();
    public final Collection<RatingItem> generalRating = new ConcurrentLinkedQueue<RatingItem>();
    public final Collection<RatingItem> weeklyRating = new ConcurrentLinkedQueue<RatingItem>();
    public final Collection<String> history = new ConcurrentLinkedQueue<String>();
    public final Collection<String> friends = new ConcurrentLinkedQueue<String>();
    public final Map<Ability, Integer> abilityExpireTime = new ConcurrentHashMap<Ability, Integer>(4); // ....

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
    private static final int SKILL_OFFSET = 0x20;
    private static final int HISTORY_MAX = 50;
    private static final String SETTINGS_FILE = "settings";
    private static final String HISTORY_FILE = "history";

    // ============================
    // === USUAL PRIVATE FIELDS ===
    // ============================

    private ISender sender;
    private IFileReader fileReader;
    private int enemySid = 0;
    private boolean aggressor = true;

    // ==========================
    // === NON-SERVER METHODS ===
    // ==========================

    /**
     * Sets a new sender to the model
     * @param sender - sender (may be NULL)
     */
    public void setSender(ISender sender) {
        this.sender = sender;
    }

    /**
     * Sets a new file reader to the model
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
                if (settings.length >= 2) {
                    languageEn = settings[0].equals("e");
                    notifyNewBattles = settings[1].equals("1");
                }
            }
        }
    }

    /**
     * Saves current settings to the internal file
     */
    public void saveSettings() {
        if (fileReader != null) {
            String s = String.format("%s %s", languageEn ? "e" : "r", notifyNewBattles ? "1" : "0");
            fileReader.write(SETTINGS_FILE, s);
        }
    }

    /**
     * @param ability - ability (if NULL then returns empty list)
     * @return collection of available products by the given ability (e.g. Snorkel for 1 day, 3 days, 7 days)
     */
    public Collection<Product> getProductsByAbility(Ability ability) {
        List<Product> res = new LinkedList<Product>();
        for (Product product : products) {  // pity it's not Java 1.8
            if (product.ability == ability)
                res.add(product);
        }
        return res;
    }

    // ==============================
    // === SERVER REQUEST METHODS ===
    // ==============================
    // Feel free to call these methods from anywhere
    // ==============================

    /**
     * Sends SIGN_IN command to the server
     * @param login - user name
     * @param password - password
     */
    public void signIn(String login, String password) {
        if (sender != null) {
            sender.send(SIGN_IN, String.format("\1%s\0%s", login, password).getBytes()); // \1 = Local auth
        }
    }

    /**
     * Sends SIGN_UP command to the server
     * @param login - user name
     * @param password - password
     * @param email - email address
     */
    public void signUp(String login, String password, String email) {
        if (sender != null) {
            sender.send(SIGN_UP, String.format("%s\0%s\0%s", login, password, email).getBytes());
        }
    }

    /**
     * Sends SIGN_OUT command to the server
     */
    public void signOut() {
        if (sender != null) {
            sender.send(SIGN_OUT);
        }
    }

    /**
     * Sends INVITE command to the server (by name)
     * @param victim - victim user name
     */
    public void invite(String victim) {
        if (sender != null) {
            aggressor = true;
            enemy = victim;
            sender.send(ATTACK, String.format("\0%s", victim).getBytes());
        }
    }

    /**
     * Sends INVITE command to the server (latest enemy)
     */
    public void inviteLatest() {
        if (sender != null) {
            aggressor = true;
            sender.send(ATTACK, 1);
        }
    }

    /**
     * Sends INVITE command to the server (random enemy)
     */
    public void inviteRandom() {
        if (sender != null) {
            aggressor = true;
            sender.send(ATTACK, 2);
        }
    }

    /**
     * Sends ACCEPT command to the server (in response to INVITE)
     */
    public void accept() {
        if (sender != null) {
            aggressor = false;
            sender.send(ACCEPT, new byte[]{(byte) (enemySid / 256), (byte) (enemySid % 256)});
        }
    }

    /**
     * Sends REJECT command to the server (in response to INVITE)
     */
    public void reject() {
        if (sender != null) {
            sender.send(REJECT, new byte[]{(byte) (enemySid / 256), (byte) (enemySid % 256)});
        }
    }

    /**
     * Sends FRIEND_LIST command to the server
     */
    public void getFriends() {
        if (sender != null) {
            sender.send(FRIEND_LIST);
        }
    }

    /**
     * Sends ADD_FRIEND command to the server
     * @param name - friend user name
     */
    public void addFriend(String name) {
        if (sender != null) {
            sender.send(ADD_FRIEND, name.getBytes());
        }
    }

    /**
     * Sends REMOVE_FRIEND command to the server
     * @param name - quondam friend name
     */
    public void removeFriend(String name) {
        if (sender != null) {
            sender.send(REMOVE_FRIEND, name.getBytes());
        }
    }

    /**
     * Sends RATING command to the server
     * @param type - type of rating (General, Weekly, etc.)
     */
    public void getRating(RatingType type) {
        assert type != null;
        if (sender != null) {
            sender.send(RATING, type.ordinal());
        }
    }

    /**
     * Sends BUY_PRODUCT command to the server
     * @param product - product to buy
     */
    public void buyProduct(Product product) {
        assert product != null;
        if (sender != null) {
            sender.send(BUY_PRODUCT, new byte[]{(byte) product.ability.ordinal(), (byte) product.days});
        }
    }

    /**
     * Sends MOVE_LEFT battle command to the server
     */
    public void moveLeft() {
        if (sender != null && curActor != null) {
            if (curActor.getX() > 0)
                sender.send(MOVE_LEFT);
        }
    }

    /**
     * Sends MOVE_RIGHT battle command to the server
     */
    public void moveRight() {
        if (sender != null && curActor != null) {
            if (curActor.getX() < Field.WIDTH - 1)
                sender.send(MOVE_RIGHT);
        }
    }

    /**
     * Sends MOVE_UP battle command to the server
     */
    public void moveUp() {
        if (sender != null && curActor != null) {
            if (curActor.getY() > 0)
                sender.send(MOVE_UP);
        }
    }

    /**
     * Sends MOVE_DOWN battle command to the server
     */
    public void moveDown() {
        if (sender != null && curActor != null) {
            if (curActor.getY() < Field.HEIGHT - 1)
                sender.send(MOVE_DOWN);
        }
    }

    /**
     * Sends USE_THING battle command to the server
     */
    public void useThing() {
        if (sender != null && curThing != null) {
            sender.send(USE_THING, curThing.getId());
        }
    }

    /**
     * Sends USE_SKILL battle command to the server
     * @param ability - ability to use (it must be a SKILL, i.e. has a number > SKILL_OFFSET)
     */
    public void useAbility(Ability ability) {
        assert ability != null;
        if (sender != null) {
            if (ability.ordinal() > SKILL_OFFSET) // only skills may be used
                sender.send(USE_SKILL, ability.ordinal());
        }
    }

    // ===============================
    // === SERVER RESPONSE METHODS ===
    // ===============================
    // These methods are not expected to be called from external code
    // ===============================

    public void setAuthorized(boolean value) {
        authorized = value;
        if (sender != null) {
            if (authorized) {
                sender.send(USER_INFO);
                sender.send(RANGE_OF_PRODUCTS);
                sender.send(FRIEND_LIST); // without this "InviteByName" dialog suggests add everyone to friends
            } else sender.resetSid();
        }
    }

    public synchronized void setUserInfo(int[] data) {
        assert data != null;
        int i = 0;

        // parse name
        StringBuilder bld = new StringBuilder();
        for (; i < data.length && data[i] != 0; i++) {
            bld.append((char) data[i]);
        }
        name = bld.toString();
        i++;

        // parse crystals
        if (i + 3 < data.length)
            crystals = (data[i] << 24) | (data[i + 1] << 16) | (data[i + 2] << 8) | (data[i + 3]); // what if > 2*10^9?
        i += 4;

        // parse abilities
        Ability[] array = Ability.values();
        abilityExpireTime.clear();
        int abilitiesCnt = data[i++];
        for (int j = 0; j < abilitiesCnt; j++, i += 3) {
            if (i + 2 < data.length) {
                int id = data[i];
                int minutes = data[i + 1] * 256 + data[i + 2];
                if (0 <= id && id < array.length)
                    abilityExpireTime.put(array[id], minutes);
            }
        }

        // read the history from a local storage
        if (fileReader != null) {
            String strHistory = fileReader.read(String.format("%s_%s", HISTORY_FILE, name));
            if (strHistory != null) {
                history.clear();
                Collections.addAll(history, strHistory.split("\n"));
            }
        }
    }

    public void attacked(int sid, String aggressorName) {
        assert aggressorName != null;
        enemySid = sid;
        enemy = aggressorName;
        inviteTime = System.currentTimeMillis();
    }

    public void refusedRejected(String coward) {
        assert coward != null;
        enemy = coward;
        refusedRejectedTime = System.currentTimeMillis();
    }

    public void refusedMissed(String coward) {
        assert coward != null;
        enemy = coward;
        refusedMissedTime = System.currentTimeMillis();
    }

    public synchronized void setFriendList(int[] data) {
        assert data != null;
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        friends.clear();
        Collections.addAll(friends, new String(bytes).split("\0"));
    }

    public void friendAdded(String name) {
        friends.add(name);
    }

    public void friendRemoved(String name) {
        friends.remove(name);
    }

    public synchronized void setRangeOfProducts(final int[] data) {
        assert data != null;
        Ability[] abs = Ability.values();
        products.clear();
        for (int i = 0; i + 2 < data.length; i += 3) {
            int id = data[i];
            int days = data[i + 1];
            int cost = data[i + 2];
            if (0 <= id && id < abs.length)
                products.add(new Product(abs[id], days, cost));
        }
    }

    public synchronized void setRating(RatingType type, int[] data) {
        assert type != null && data != null;
        Collection<RatingItem> rating = type == RatingType.General ? generalRating : weeklyRating;
        rating.clear();

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

        // update current rating time to make the 'subscribers' update their states
        if (type == RatingType.General)
            generalRatingTime = System.currentTimeMillis();
        else if (type == RatingType.Weekly)
            weeklyRatingTime = System.currentTimeMillis();
    }

    public void setNewField(int[] fieldData) {
        curThing = null;
        curActor = null;
        field = new Field(fieldData);
    }

    public synchronized void appendObject(int number, int id, int xy) {
        assert field != null; // 'field' may be NULL! ensure method is synchronized
        field.appendObject(number, id, xy);
        if (id == AGGRESSOR_ID || id == DEFENDER_ID)
            curActor = aggressor ? field.getObject(AGGRESSOR_ID) : field.getObject(DEFENDER_ID);
    }

    public synchronized void setXy(int number, int xy) {
        assert field != null; // 'field' may be NULL! ensure method is synchronized
        field.setXy(number, xy);
    }

    public void setScore(int score1, int score2) {
        this.score1 = score1;
        this.score2 = score2;
    }

    public void setThing(int thingId) {
        curThing = Cell.newObject(thingId, 0xFF, new Field.NextNumber() {
            @Override
            public int next() {
                return 0;
            }
        });
    }

    public void roundFinished(boolean winner, int totalScore1, int totalScore2) {
        this.totalScore1 = totalScore1;
        this.totalScore2 = totalScore2;
        roundWinner = winner;
        roundFinishedTime = System.currentTimeMillis();
    }

    public synchronized void gameFinished(boolean winner) {
        // building an item
        String s = String.format(Locale.getDefault(), "%s %s %s %s (%d-%d)",
                DateFormat.getInstance().format(new Date()), enemy, aggressor ? "A" : "V", winner ? "Win" : "Loss",
                totalScore1, totalScore2);

        // prepend the item into the current history (and delete old items if necessary)
        List<String> lst = new LinkedList<String>(history);
        lst.add(0, s);
        while (lst.size() > HISTORY_MAX)
            lst.remove(HISTORY_MAX);
        history.clear();
        history.addAll(lst);

        // store the current history in the local storage
        if (fileReader != null) {
            // making a single string (oh... Java 1.8 has got "mkstring" :( )
            StringBuilder builder = new StringBuilder(lst.size());
            for (String x : lst) {
                builder.append(x).append('\n');
            }
            // writing
            fileReader.write(String.format("%s_%s", HISTORY_FILE, name), builder.toString());
        }

        // reset reference to a field
        field = null;
        gameFinishedTime = System.currentTimeMillis();
    }

    public synchronized void setAbilities(int[] ids) {
        assert ids != null;
        abilities.clear();
        Ability[] array = Ability.values();
        for (int id : ids) {
            if (0 <= id && id < array.length)
                abilities.add(array[id]);
        }
    }
}
