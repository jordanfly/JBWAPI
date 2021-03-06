package bwapi;

import bwapi.ClientData.PlayerData;
import java.util.Set;
import java.util.stream.Collectors;

import static bwapi.TextColor.*;
import static bwapi.UnitType.*;
import static bwapi.UpgradeType.*;
import static bwapi.WeaponType.*;

public class Player {
    private final PlayerData playerData;
    private final Game game;
    private final int id;
    private final String name;
    private final Race race;
    private final PlayerType playerType;
    private final Force force;
    private final TilePosition startLocation;

    Player(final PlayerData playerData, final int id, final Game game) {
        this.playerData = playerData;
        this.game = game;
        this.id = id;
        this.name = playerData.getName();
        this.race = Race.races[playerData.getRace()];
        this.playerType = PlayerType.idToEnum[playerData.getType()];
        this.force = game.getForce(playerData.getForce());
        this.startLocation = new TilePosition(playerData.getStartLocationX(), playerData.getStartLocationY());
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Unit> getUnits() {
        return game.getAllUnits().stream()
                .filter(u -> equals(u.getPlayer()))
                .collect(Collectors.toSet());
    }

    public Race getRace() {
        return race;
    }

    public PlayerType getType() {
        return playerType;
    }

    public Force getForce() {
        return force;
    }

    public boolean isAlly(final Player player) {
        return playerData.isAlly(player.getID());
    }

    //TODO FIX OBSERVERS in 4.3.0
    public boolean isEnemy(final Player player) {
        if (player.isNeutral()) {
            return false;
        }
        return !isAlly(player);
    }

    public boolean isNeutral() {
        return  equals(game.neutral());
    }

    public TilePosition getStartLocation() {
        return startLocation;
    }

    public boolean isVictorious() {
        return playerData.isVictorious();
    }

    public boolean isDefeated() {
        return playerData.isDefeated();
    }

    public boolean leftGame() {
        return playerData.getLeftGame();
    }

    public int minerals() {
        return playerData.getMinerals();
    }

    public int gas() {
        return playerData.getGas();
    }

    public int gatheredMinerals() {
        return playerData.getGatheredMinerals();
    }

    public int gatheredGas() {
        return playerData.getGatheredGas();
    }

    public int repairedMinerals() {
        return playerData.getRepairedMinerals();
    }

    public int repairedGas() {
        return playerData.getRepairedGas();
    }

    public int refundedMinerals() {
        return playerData.getRefundedMinerals();
    }

    public int refundedGas() {
        return playerData.getRefundedGas();
    }

    public int spentMinerals() {
        return gatheredMinerals() + refundedMinerals() - minerals() - repairedMinerals();
    }

    public int spentGas() {
        return gatheredGas() + refundedGas() - gas() - repairedGas();
    }

    public int supplyTotal() {
        return supplyTotal(getRace());
    }

    public int supplyTotal(final Race race) {
        return playerData.getSupplyTotal(race.id);
    }

    public int supplyUsed() {
        return supplyUsed(getRace());
    }

    public int supplyUsed(final Race race) {
        return playerData.getSupplyUsed(race.id);
    }

    public int allUnitCount() {
        return allUnitCount(UnitType.AllUnits);
    }

    public int allUnitCount(final UnitType unit) {
        return playerData.getAllUnitCount(unit.id);
    }

    public int visibleUnitCount() {
        return visibleUnitCount(UnitType.AllUnits);
    }

    public int visibleUnitCount(final UnitType unit) {
        return playerData.getVisibleUnitCount(unit.id);
    }

    public int completedUnitCount() {
        return completedUnitCount(UnitType.AllUnits);
    }

    public int completedUnitCount(final UnitType unit) {
        return playerData.getCompletedUnitCount(unit.id);
    }

    public int incompleteUnitCount() {
        return allUnitCount() - completedUnitCount();
    }

    public int incompleteUnitCount(final UnitType unit) {
        return allUnitCount(unit) - completedUnitCount(unit);
    }

    public int deadUnitCount() {
        return deadUnitCount(UnitType.AllUnits);
    }

    public int deadUnitCount(final UnitType unit) {
        return playerData.getDeadUnitCount(unit.id);
    }

    public int killedUnitCount() {
        return killedUnitCount(UnitType.AllUnits);
    }

    public int killedUnitCount(final UnitType unit) {
        return playerData.getKilledUnitCount(unit.id);
    }

    public int getUpgradeLevel(final UpgradeType upgrade) {
        return playerData.getUpgradeLevel(upgrade.id);
    }

    public boolean hasResearched(final TechType tech) {
        return playerData.getHasResearched(tech.id);
    }

    public boolean isResearching(final TechType tech) {
        return playerData.isResearching(tech.id);
    }

    public boolean isUpgrading(final UpgradeType upgrade) {
        return playerData.isUpgrading(upgrade.id);
    }

    public Color getColor() {
        return new Color(playerData.getColor());
    }

    public TextColor getTextColor() {
        switch (playerData.getColor()) {
            case 111: // red
                return BrightRed;
            case 165: // blue
                return Blue;
            case 159: // teal
                return Teal;
            case 164: // purp
                return Purple;
            case 179: // oj
                return Orange;
            case 19:  // brown
                return Brown;
            case 84:  // white
                return PlayerWhite;
            case 135: // yellow
                return PlayerYellow;
            case 185: // green p9
                return DarkGreen;
            case 136: // p10
                return LightYellow;
            case 134: // p11
                return Tan;
            case 51:  // p12
                return GreyBlue;
            default:
                return Default;
        }
    }

    public int maxEnergy(final UnitType unit) {
        int energy = unit.maxEnergy();
        if (unit == Protoss_Arbiter && getUpgradeLevel(Khaydarin_Core) > 0 ||
                unit == Protoss_Corsair && getUpgradeLevel(Argus_Jewel) > 0 ||
                unit == Protoss_Dark_Archon && getUpgradeLevel(Argus_Talisman) > 0 ||
                unit == Protoss_High_Templar && getUpgradeLevel(Khaydarin_Amulet) > 0 ||
                unit == Terran_Ghost && getUpgradeLevel(Moebius_Reactor) > 0 ||
                unit == Terran_Battlecruiser && getUpgradeLevel(Colossus_Reactor) > 0 ||
                unit == Terran_Science_Vessel && getUpgradeLevel(Titan_Reactor) > 0 ||
                unit == Terran_Wraith && getUpgradeLevel(Apollo_Reactor) > 0 ||
                unit == Terran_Medic && getUpgradeLevel(Caduceus_Reactor) > 0 ||
                unit == Zerg_Defiler && getUpgradeLevel(Metasynaptic_Node) > 0 ||
                unit == Zerg_Queen && getUpgradeLevel(Gamete_Meiosis) > 0) {
            energy += 50;
        }
        return energy;
    }

    public double topSpeed(final UnitType unit) {
        double speed = unit.topSpeed();
        if (unit == Terran_Vulture && getUpgradeLevel(Ion_Thrusters) > 0 ||
                unit == Zerg_Overlord && getUpgradeLevel(Pneumatized_Carapace) > 0 ||
                unit == Zerg_Zergling && getUpgradeLevel(Metabolic_Boost) > 0 ||
                unit == Zerg_Hydralisk && getUpgradeLevel(Muscular_Augments) > 0 ||
                unit == Protoss_Zealot && getUpgradeLevel(Leg_Enhancements) > 0 ||
                unit == Protoss_Shuttle && getUpgradeLevel(Gravitic_Drive) > 0 ||
                unit == Protoss_Observer && getUpgradeLevel(Gravitic_Boosters) > 0 ||
                unit == Protoss_Scout && getUpgradeLevel(Gravitic_Thrusters) > 0 ||
                unit == Zerg_Ultralisk && getUpgradeLevel(Anabolic_Synthesis) > 0) {
            if (unit == Protoss_Scout) {
                speed += 427 / 256.0;
            } else {
                speed = speed * 1.5;
            }
            if (speed < 853 / 256.0) {
                speed = 853 / 256.0;
            }
            //acceleration *= 2;
            //turnRadius *= 2;
        }
        return speed;
    }

    public int weaponMaxRange(final WeaponType weapon) {
        int range = weapon.maxRange();
        if (weapon == Gauss_Rifle && getUpgradeLevel(U_238_Shells) > 0 ||
                weapon == Needle_Spines && getUpgradeLevel(Grooved_Spines) > 0) {
            range += 1 * 32;
        } else if (weapon == Phase_Disruptor && getUpgradeLevel(Singularity_Charge) > 0) {
            range += 2 * 32;
        } else if (weapon == Hellfire_Missile_Pack && getUpgradeLevel(Charon_Boosters) > 0) {
            range += 3 * 32;
        }
        return range;
    }

    public int sightRange(final UnitType unit) {
        int range = unit.sightRange();
        if (unit == Terran_Ghost && getUpgradeLevel(Ocular_Implants) > 0 ||
                unit == Zerg_Overlord && getUpgradeLevel(Antennae) > 0 ||
                unit == Protoss_Observer && getUpgradeLevel(Sensor_Array) > 0 ||
                unit == Protoss_Scout && getUpgradeLevel(Apial_Sensors) > 0) {
            range = 11 * 32;
        }
        return range;
    }

    public int weaponDamageCooldown(final UnitType unit) {
        int cooldown = unit.groundWeapon().damageCooldown();
        if (unit == Zerg_Zergling && getUpgradeLevel(Adrenal_Glands) > 0) {
            // Divide cooldown by 2
            cooldown /= 2;
            // Prevent cooldown from going out of bounds
            cooldown = Math.min(Math.max(cooldown, 5), 250);
        }
        return cooldown;
    }

    public int armor(final UnitType unit) {
        int armor = unit.armor();
        armor += getUpgradeLevel(unit.armorUpgrade());
        if ((unit == Zerg_Ultralisk && getUpgradeLevel(Chitinous_Plating) > 0) || unit == Hero_Torrasque) {
            armor += 2;
        }
        return armor;
    }

    public int damage(final WeaponType wpn) {
        int dmg = wpn.damageAmount();
        dmg += getUpgradeLevel(wpn.upgradeType()) * wpn.damageBonus();
        dmg *= wpn.damageFactor();
        return dmg;
    }

    public int getUnitScore() {
        return playerData.getTotalUnitScore();
    }

    public int getKillScore() {
        return playerData.getTotalKillScore();
    }

    public int getBuildingScore() {
        return playerData.getTotalBuildingScore();
    }

    public int getRazingScore() {
        return playerData.getTotalRazingScore();
    }

    public int getCustomScore() {
        return playerData.getCustomScore();
    }

    public boolean isObserver() {
        return !playerData.isParticipating();
    }

    public int getMaxUpgradeLevel(final UpgradeType upgrade) {
        return playerData.getMaxUpgradeLevel(upgrade.id);
    }

    public boolean isResearchAvailable(final TechType tech) {
        return playerData.isResearchAvailable(tech.id);
    }

    public boolean isUnitAvailable(final UnitType unit) {
        return playerData.isUnitAvailable(unit.id);
    }

    public boolean hasUnitTypeRequirement(final UnitType unit) {
        return hasUnitTypeRequirement(unit, 1);
    }

    public boolean hasUnitTypeRequirement(final UnitType unit, final int amount) {
        if (unit == UnitType.None) {
            return true;
        }

        switch (unit) {
            case Zerg_Hatchery:
                return completedUnitCount(Zerg_Hatchery) + allUnitCount(Zerg_Lair) + allUnitCount(Zerg_Hive) >= amount;
            case Zerg_Lair:
                return completedUnitCount(Zerg_Lair) + allUnitCount(Zerg_Hive) >= amount;
            case Zerg_Spire:
                return completedUnitCount(Zerg_Spire) + allUnitCount(Zerg_Greater_Spire) >= amount;
            default:
                return completedUnitCount(unit) >= amount;
        }
    }

    public boolean equals(final Object that) {
        if (!(that instanceof Player)) {
            return false;
        }
        return getID() == ((Player) that).getID();
    }

    public int hashCode() {
        return getID();
    }
}
