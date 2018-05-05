package ru.mitrakov.self.rush.model.emulator;

import java.util.*;

import ru.mitrakov.self.rush.model.Model;
import ru.mitrakov.self.rush.net.IHandler;
import ru.mitrakov.self.rush.GcResistantIntArray;
import ru.mitrakov.self.rush.utils.collections.IIntArray;

import static ru.mitrakov.self.rush.model.Model.Character.*;
import static ru.mitrakov.self.rush.model.Model.abilityValues;

/**
 * Created by mitrakov on 04.02.2018
 */
public class ServerEmulator {
    private final Model model;
    private final IHandler handler;
    private final BattleManager battleManager;
    private final IIntArray abilities1 = new GcResistantIntArray(10);
    private final IIntArray abilities2 = new GcResistantIntArray(0);
    private final List<Model.Character> characters = new ArrayList<Model.Character>(Model.characterValues.length);

    public ServerEmulator(Model model, Model.IFileReader fileReader, IHandler handler) {
        assert model != null && fileReader != null && handler != null;
        this.model = model;
        this.handler = handler;
        this.battleManager = new BattleManager(this, fileReader);
    }

    public void send(IIntArray data) {
        assert data != null;

        if (data.length() > 0) {
            int code = data.get(0);
            if (0 <= code && code < Model.cmdValues.length) {
                Model.Cmd cmd = Model.cmdValues[code];
                switch (cmd) {
                    case ATTACK:
                        boolean isInviteByName = data.length() > 1 && data.get(1) == 0;
                        if (isInviteByName)
                            attack(data.remove(0, 2).toUTF8());
                        else throw new IllegalArgumentException("ATTACK: incorrect ATTACK format");
                        break;
                    case MOVE:
                        if (data.length() > 1)
                            move(data.get(1));
                        else throw new IllegalArgumentException("MOVE: direction must be provided");
                        break;
                    case USE_THING:
                        useThing();
                        break;
                    case USE_SKILL:
                        if (data.length() > 1)
                            useSkill(data.get(1));
                        else throw new IllegalArgumentException("USE_SKILL: ability must be provided");
                        break;
                    case USER_INFO:
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Cmd %s not supported by Emulator", cmd));
                }
            }
        } else throw new IllegalArgumentException("Empty data");
    }

    void receive(IIntArray data) {
        handler.onReceived(data.prepend(data.length() % 256).prepend(data.length() / 256));
    }

    void gameOver(boolean winner) {
        model.moveForwardSinglePlayerProgress(winner);
    }

    private void attack(String levelName) {
        abilities1.clear();
        for (Model.Ability ability : model.userAbilities) {
            int code = Arrays.binarySearch(abilityValues, ability); // don't use "ability.ordinal()" (GC pressure)
            abilities1.add(code);
        }
        battleManager.accept(model.character, getEnemyCharacter(), abilities1, abilities2, new String[]{levelName}, 1);
    }

    private void move(int direction) {
        battleManager.move(direction);
    }

    private void useThing() {
        battleManager.useThing();
    }

    private void useSkill(int skillId) {
        battleManager.useSkill(skillId);
    }

    private synchronized Model.Character getEnemyCharacter() {
        characters.clear();
        for (int i = 0; i < Model.characterValues.length; i++) {
            Model.Character character = Model.characterValues[i];
            if (character != None && character != model.character)
                characters.add(character);
        }
        assert characters.size() > 0;
        Collections.shuffle(characters);
        return characters.get(0);
    }
}
