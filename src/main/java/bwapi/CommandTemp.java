package bwapi;

import java.util.List;

import static bwapi.UnitCommandType.*;

class CommandTemp {
    private final UnitCommand command;
    private final Game game;

    private enum EventType { Order, Resource, Finish };
    private EventType eventType = EventType.Resource;
    private int savedExtra = -1;
    private int savedExtra2 = -1;

    private Player player = null;

    CommandTemp(final UnitCommand command, final Game game) {
        this.command = command;
        this.game = game;
    }

    private static void addToBuffer(final List<List<CommandTemp>> buf, final CommandTemp command, final int frames) {
        command.execute(frames == 0);
        buf.get(frames - 1).add(command);
    }

    void insertIntoCommandBuffer(List<List<CommandTemp>> buf)  {
        final CommandTemp orderEvent = makeEvent(EventType.Order);
        final CommandTemp finishEvent = makeEvent(EventType.Finish);

        final int remainingLatencyFrames = game.getRemainingLatencyFrames();
        switch (command.type) {
            // RLF: Resource event
            // RLF + 1: Order event
            // RLF + 2: Finish event
            case Cancel_Construction:
                addToBuffer(buf,this, remainingLatencyFrames);
                addToBuffer(buf, orderEvent, remainingLatencyFrames + 1);
                addToBuffer(buf, finishEvent, remainingLatencyFrames + 2);
                break;

            // RLF: Resource event
            // RLF + 1: Order event
            case Build_Addon:
            case Cancel_Addon:
            case Cancel_Research:
            case Cancel_Upgrade:
            case Morph:
                addToBuffer(buf, this, remainingLatencyFrames);
                addToBuffer(buf, orderEvent, remainingLatencyFrames + 1);
                break;

            // RLF: Resource event
            // RLF + 1: Order event   (only for building . building morphs)
            // RLF + 13: Finish event (only for unit     . unit morphs)
            // RLF + 15: Finish event (only for building . building morphs)
            case Cancel_Morph:
                addToBuffer(buf, this, remainingLatencyFrames);

                if (command.unit != null && command.unit.getType().isBuilding()) {
                    addToBuffer(buf, orderEvent,  remainingLatencyFrames + 1);
                    addToBuffer(buf, finishEvent, remainingLatencyFrames + 15);
                }
                else {
                    addToBuffer(buf, finishEvent, remainingLatencyFrames + 13);
                }
                break;

            // RLF: Resource event
            // RLF + 1: Order event
            // RLF + 3: Finish event
            case Cancel_Train:
            case Cancel_Train_Slot:
                addToBuffer(buf, this, remainingLatencyFrames);
                addToBuffer(buf, orderEvent,  remainingLatencyFrames + 1);
                addToBuffer(buf, finishEvent, remainingLatencyFrames + 3);
                break;

            // RLF: Order event
            // RLF + 1: Finish event
            case Halt_Construction:
                eventType = EventType.Order;
                addToBuffer(buf, this, remainingLatencyFrames);
                addToBuffer(buf, finishEvent, remainingLatencyFrames + 1);
                break;

            default:
                addToBuffer(buf, this, remainingLatencyFrames);
                break;
        }

    }
    private CommandTemp makeEvent(final EventType type) {
        CommandTemp other = new CommandTemp(command, game);
        other.eventType = type;
        return other;
    }

    private static int getUnitID(Unit unit) {
        if ( unit == null) {
            return -1;
        }
        return unit.getID();
    }

    void execute() {
        switch(command.type) {
            case Halt_Construction:
                eventType = EventType.Order;
            default:
                execute(game.getRemainingLatencyFrames() == 0);
                break;
        }
    }
    void execute(boolean isCurrentFrame) {
        // Immediately return if latency compensation is disabled or if the command was queued
        if (!game.isLatComEnabled() || command.isQueued()) {
            return;
        }
        Unit unit   = command.unit;
        Unit target = command.target;

        if (isCurrentFrame) {
            switch (command.type) { // Commands which do things during the current frame

                case Morph:       // Morph, Build_Addon and Train orders may reserve resources or supply that
                case Build_Addon: // SC does not take until the next frame to protect bots from overspending.
                case Train:
                    if(eventType == EventType.Resource)
                        break;
                    return;
                default:
                    return;
            }
        }

        // Get the player (usually the unit's owner)
        if (player == null) {
            player = unit != null ? unit.getPlayer() : game.self();
        }
        // Existence test
        if (!unit.self.exists) {
            return;
        }

        // Move test
        switch (command.type) {
            case Follow:
            case Hold_Position:
            case Move:
            case Patrol:
            case Right_Click_Position:
            case Attack_Move:
                if (!unit.getType().canMove()) {
                    return;

                }
                break;
            default:
                break;
        }

        // Apply command changes
        switch(command.type) {
            // RLF
            case Attack_Move:
                unit.self.order                = Order.AttackMove;
                unit.self.targetPositionX      = command.x;
                unit.self.targetPositionY      = command.y;
                unit.self.orderTargetPositionX = command.x;
                unit.self.orderTargetPositionY = command.y;
                break;

            // RLF
            case Attack_Unit:
                if (target == null|| !target.self.exists || !unit.getType().canAttack())
                return;
                unit.self.order  = Order.AttackUnit;
                unit.self.target = getUnitID(target);
                break;

            // RLF
            case Build:
                unit.self.order          = Order.PlaceBuilding;
                unit.self.isConstructing = true;
                unit.self.isIdle         = false;
                unit.self.buildType      = command.extra;
                break;

            // For building addons, SC takes minerals on RLF + 1.
            // Latcom will do as with building.building morph and reserve these resources.
            // RLF: Resource event
            // RLF + 1: Order event
            case Build_Addon: {
                UnitType addonType = UnitType.values()[command.extra];
                switch (eventType) {
                    case Resource:
                        player.self.minerals -= addonType.mineralPrice();
                        player.self.gas      -= addonType.gasPrice();

                        if (!isCurrentFrame) { // We will pretend the building is busy building, this doesn't
                            unit.self.isIdle = false;
                            unit.self.order = Order.PlaceAddon;
                        }
                        break;

                    case Order:
                    unit.self.isConstructing = true;
                    unit.self.order          = Order.Nothing;
                    unit.self.secondaryOrder = Order.BuildAddon;
                    unit.self.buildType      = command.extra;
                    break;
                    }
            }
            break;

            // RLF
            case Burrow:
                unit.self.order = Order.Burrowing;
                break;

            // RLF: Resource event
            // RLF + 1: Order event
            case Cancel_Addon:
                switch(eventType) {
                    case Resource:
                        {
                            UnitType addonType = unit.self.buildType;
                            player.self.minerals += addonType.mineralPrice() * 0.75;
                            player.self.gas += addonType.gasPrice() * 0.75;
                            unit.self.buildType = UnitType.None;
                        }
                        break;
                    case Order:
                        unit.self.remainingBuildTime = 0;
                        unit.self.isConstructing = false;
                        unit.self.order = Order.Nothing;
                        unit.self.isIdle = true;
                        unit.self.buildUnit = -1;
                        break;
                }
                break;

            // RLF: Resource event
            // RLF + 1: Order event
            // RLF + 2: Finish event
            case Cancel_Construction: {
                if (unit.getType().getRace() == Race.Terran) {
                    Unit builder = game.getUnit(unit.self.buildUnit);
                    if (builder != null && builder.exists()) {
                        switch (eventType) {
                            case Resource:
                                builder.self.buildType = UnitType.None;
                                break;
                            case Order:
                                builder.self.isConstructing = false;
                                builder.self.order = Order.ResetCollision;
                                break;
                            case Finish:
                                builder.self.order = Order.PlayerGuard;
                                break;
                        }
                    }
                }

                if (eventType == EventType.Resource) {
                    unit.self.buildUnit = -1;
                    player.self.minerals += unit.getType().mineralPrice() * 0.75;
                    player.self.gas += unit.getType().gasPrice()     * 0.75;
                    unit.self.remainingBuildTime = 0;
                }

                if (unit.getType().getRace() == Race.Zerg) {
                    switch (eventType) {
                        case Resource:
                            unit.self.type           = unit.getType().whatBuilds().getKey();
                            unit.self.buildType      = UnitType.None;
                            unit.self.isMorphing     = false;
                            unit.self.order          = Order.ResetCollision;
                            unit.self.isConstructing = false;

                            player.self.supplyUsed[unit.getType().getRace()] += unit.getType().supplyRequired();
                            break;

                        case Order:
                            unit.self.order  = Order.PlayerGuard;
                            unit.self.isIdle = true;
                            break;
                    }
                }

                break;
            }


            // RLF: Resource event
            // RLF + 1: Order event (only for builing . building morphs)
            // RLF + 13: Finish event (only for unit . unit morphs)
            // RLF + 15: Finish event (only for building . building morphs)
            case Cancel_Morph:
                switch(eventType) {
                    case Resource: {
                        UnitType builtType = unit.self.buildType;
                        UnitType newType = builtType.whatBuilds().getKey();

                        if (newType.isBuilding()) {
                            player.self.minerals += builtType.mineralPrice() * 0.75;
                            player.self.gas      += builtType.gasPrice() * 0.75;
                        }
                        else {
                            player.self.minerals += builtType.mineralPrice();
                            player.self.gas      += builtType.gasPrice();
                        }

                        if (newType.isBuilding() && newType.producesCreep()) {
                            unit.self.order = Order.InitCreepGrowth;
                        }

                        if (unit.self.type != UnitType.Zerg_Egg) { // Issue #781
                            // https://github.com/bwapi/bwapi/issues/781
                            unit.self.type = newType;
                        }

                        unit.self.buildType = UnitType.None;
                        unit.self.isConstructing = false;
                        unit.self.isMorphing = false;
                        unit.self.isCompleted = true;
                        unit.self.remainingBuildTime = 0;
                        }
                        break;

                    case Order:
                        if (unit.getType().isBuilding()) { // This event would hopefully not have been created
                                                        // if this wasn't true (see event note above)
                            unit.self.isIdle = true;
                            unit.self.order  = Order.Nothing;
                            if(unit.self.type == UnitType.Zerg_Hatchery || unit.self.type == UnitType.Zerg_Lair) {
                                // Type should have updated during last event to the cancelled type
                                unit.self.secondaryOrder = Order.SpreadCreep;
                            }
                        }
                        else {
                            player.self.supplyUsed[unit.getType().getRace()] -= unit.getType().supplyRequired() * (1 + (unit.getType().isTwoUnitsInOneEgg() ? 1 : 0));
                            // Could these races be different? Probably not.
                            // Should we handle it?            Definetely.
                            player.self.supplyUsed[unit.getType().getRace()] += unit.getType().whatBuilds().getKey().supplyRequired() * unit.getType().whatBuilds().getValue();
                            // Note: unit.getType().whatBuilds().second is always 1 but we
                            // might as well handle the general case, in case Blizzard
                            // all of a sudden allows you to cancel archon morphs
                        }

                        break;

                    case Finish:
                        if(unit.self.type == UnitType.Zerg_Hatchery || unit.self.type == UnitType.Zerg_Lair) {
                            unit.self.secondaryOrder = Order.SpawningLarva;
                        }
                        else if(!unit.getType().isBuilding()) {
                            unit.self.order          = Order.PlayerGuard;
                            unit.self.isCompleted    = true;
                            unit.self.isConstructing = false;
                            unit.self.isIdle         = true;
                            unit.self.isMorphing     = false;
                        }
                        break;
                }
                break;

            // RLF: Resource event
            // RLF + 1: Order update
            case Cancel_Research: {
                switch(eventType) {
                    case Resource: {
                        TechType techType = unit.self.tech;
                        player.self.minerals += techType.mineralPrice();
                        player.self.gas += techType.gasPrice();
                        unit.self.remainingResearchTime = 0;
                        unit.self.tech = TechType.None;
                    }
                    break;

                    case Order:
                        unit.self.order  = Order.Nothing;
                        unit.self.isIdle = true;
                        break;
                }
                }
                break;

            // RLF: Resource event
            // RLF + 1: Order event
            // RLF + 3: Finish event
            case Cancel_Train_Slot:
                if (command.extra != 0) {
                    if (eventType == EventType::Resource) {
                        UnitType unitType = unit.self.trainingQueue[command.extra];
                        player.self.minerals += unitType.mineralPrice();
                        player.self.gas += unitType.gasPrice();

                        // Shift training queue back one slot after the cancelled unit
                        for (int i = command.extra; i < 4; ++i) {
                            unit.self.trainingQueue[i] = unit.self.trainingQueue[i + 1];
                        }

                        unit.self.trainingQueueCount -= 1;
                    }
                    break;
                }

            // If we're cancelling slot 0, we fall through to Cancel_Train.
            //[[fallthrough]];

            // RLF: Resource event
            // RLF + 1: Order event
            // RLF + 3: Finish event
            case Cancel_Train: {
                switch(eventType) {
                    case Resource: {
                        UnitType unitType = unit.self.trainingQueue[unit.self.trainingQueueCount - 1];
                        player.self.minerals += unitType.mineralPrice();
                        player.self.gas += unitType.gasPrice();

                        unit.self.buildUnit = -1;

                        if (unit.self.trainingQueueCount == 1) {
                            unit.self.isIdle     = false;
                            unit.self.isTraining = false;
                        }
                        break;
                    }

                    case Order: {
                        unit.self.trainingQueueCount -= 1
                        UnitType unitType = unit.self.trainingQueue[unit.self.trainingQueueCount];
                        player.self.supplyUsed[unitType.getRace()] -= unitType.supplyRequired();

                        if (unit.self.trainingQueueCount == 0) {
                            unit.self.buildType = UnitType.None;
                        }
                        else {
                            // Actual time decreases, but we'll let it be the buildTime until latency catches up.
                            unit.self.remainingTrainTime = unit.self.trainingQueue[unit.self.trainingQueueCount - 1].buildTime();
                            unit.self.buildType = unit.self.trainingQueue[unit.self.trainingQueueCount - 1];
                        }
                    }

                    break;

                    case Finish:
                    if (unit.self.buildType == UnitType.None) {
                        unit.self.order = Order.Nothing;
                    }
                    break;
                }
                break;
            }

            // RLF: Resource event
            // RLF + 1: Order event
            case Cancel_Upgrade:
                switch(eventType) {
                    case Resource: {
                        UpgradeType upgradeType = unit.self.upgrade;
                        final int nextLevel = unit.getPlayer().getUpgradeLevel(upgradeType) + 1;

                        player.self.minerals += upgradeType.mineralPrice(nextLevel);
                        player.self.gas += upgradeType.gasPrice(nextLevel);

                        unit.self.upgrade = UpgradeType.None;
                        unit.self.remainingUpgradeTime = 0;
                        }
                        break;

                    case Order:
                        unit.self.order = Order.Nothing;
                        unit.self.isIdle  = true;
                        break;
                }
                break;

            // RLF
            case Cloak:
                unit.self.order = Order.Cloak;
                unit.self.energy -= unit.getType().cloakingTech().energyCost();
                break;

            // RLF
            case Decloak:
                unit.self.order = Order.Decloak;
                break;

            // RLF
            case Follow:
                unit.self.order = Order.Follow;
                unit.self.target = getUnitID(target);
                unit.self.isIdle = false;
                unit.self.isMoving = true;
                break;

            // RLF
            case Gather:
                unit.self.target = getUnitID(target);
                unit.self.isIdle = false;
                unit.self.isMoving = true;
                unit.self.isGathering = true;

                // @TODO: Fully time and test this order
                if (target.getType().isMineralField()) {
                    unit.self.order = Order.MoveToMinerals;
                }
                else if (target.getType().isRefinery()) {
                    unit.self.order = Order.MoveToGas;
                }

                break;

            // RLF: Order event
            // RLF + 1: Finish event
            case Halt_Construction:
                switch(eventType) {
                    case Order:
                        Unit building = game.getUnit(unit.self.buildUnit);
                        if (building != null){
                            building.self.buildUnit = -1;
                        }
                        unit.self.buildUnitb = -1;
                        unit.self.order = Order.ResetCollision;
                        unit.self.isConstructing = false;
                        unit.self.buildType = UnitType.None;
                        break;

                    case Finish:
                        unit.self.order = Order.PlayerGuard;
                        unit.self.isIdle = true;
                        break;
                }

                break;

            // RLF
            case Hold_Position:
                unit.self.isMoving = false;
                unit.self.isIdle = false;
                unit.self.order = Order.HoldPosition;
                break;

            // RLF
            case Land:
                unit.self.order = Order.BuildingLand;
                unit.self.isIdle = false;
                break;

            // RLF
            case Lift:
                unit.self.order = Order.BuildingLiftOff;
                unit.self.isIdle = false;
                break;

            // RLF
            case Load:
                if (unit.getType() == UnitType.Terran_Bunker) {
                    unit.self.order = Order.PickupBunker;
                    unit.self.target = getUnitID(target);
                }
                else if (unit.getType().spaceProvided() > 0) {
                    unit.self.order = Order.PickupTransport;
                    unit.self.target = getUnitID(target);
                }
                else if (target.getType().spaceProvided() > 0) {
                    unit.self.order = Order.EnterTransport;
                    unit.self.target = getUnitID(target);
                }
                unit.self.isIdle = false;
    
                break;

            // For morph, SC takes minerals on RLF + 1 if morphing building.building.
            // Latcom will do as with addons and reserve these resources.
            // RLF: Resource event
            // RLF + 1: Order event
            case Morph: {
                UnitType morphType = UnitType.values()[command.extra];
    
                switch (eventType) {
                    case Resource:
                        if(!isCurrentFrame) {
                            unit.self.isCompleted = false;
                            unit.self.isIdle = false;
                            unit.self.isConstructing = true;
                            unit.self.isMorphing = true;
                            unit.self.buildType = morphType;
                        }
            
                        if (unit.getType().isBuilding()) {
                            if (!isCurrentFrame) { // Actions that don't happen when we're reserving resources
                                unit.self.order = Order.ZergBuildingMorph;
                                unit.self.type = morphType;
                            }
                            player.self.minerals -= morphType.mineralPrice();
                            player.self.gas -= morphType.gasPrice();
                        }
                        else {
                            player.self.supplyUsed[morphType.getRace()] += morphType.supplyRequired() * (1 + (morphType.isTwoUnitsInOneEgg() ? 1 : 0)) - unit.getType().supplyRequired();
                
                            if(!isCurrentFrame) {
                                unit.self.order = Order.ZergUnitMorph;
                    
                                player.self.minerals -= morphType.mineralPrice();
                                player.self.gas      -= morphType.gasPrice();
                    
                                switch(morphType) {
                                    case Zerg_Lurker_Egg:
                                        unit.self.type = UnitType.Zerg_Lurker_Egg;
                                        break;
                    
                                    case Zerg_Devourer:
                                    case Zerg_Guardian:
                                        unit.self.type = UnitType.Zerg_Cocoon;
                                        break;
                
                                    default:
                                        unit.self.type = UnitType.Zerg_Egg;
                                        break;
                                }
                    
                                unit.self.trainingQueue[unit.self.trainingQueueCount++] = morphType;
                            }
                        }
                        break;
                        case Order:
                            if (unit.getType().isBuilding()) {
                                unit.self.order = Order.IncompleteBuilding;
                            }
                        break;
                }
            }

            break;

            // RLF
            case Move:
                unit.self.order = Order.Move;
                unit.self.targetPositionX = command.x;
                unit.self.targetPositionY = command.y;
                unit.self.orderTargetPositionX = command.x;
                unit.self.orderTargetPositionY = command.y;
                unit.self.isMoving = true;
                unit.self.isIdle = false;
                break;

            // RLF
            case Patrol:
                unit.self.order = Order.Patrol;
                unit.self.isIdle = false;
                unit.self.isMoving = true;
                unit.self.targetPositionX = command.x;
                unit.self.targetPositionY = command.y;
                unit.self.orderTargetPositionX = command.x;
                unit.self.orderTargetPositionY = command.y;
                break;

            // RLF
            case Repair:
                if (unit.getType() != UnitType.Terran_SCV) {
                    return;
                }
                unit.self.order = Order.Repair;
                unit.self.target = getUnitID(target);
                unit.self.isIdle = false;
                break;

            // RLF
            case Research: {
                TechType techType = TechType.values()[command.extra];
                unit.self.order = Order.ResearchTech;
                unit.self.tech = techType;
                unit.self.isIdle = false;
                unit.self.remainingResearchTime = techType.researchTime();

                player.self.minerals -= techType.mineralPrice();
                player.self.gas -= techType.gasPrice();
                player.self.isResearching[techType] = true;
                }
                break;

            // RLF
            case Return_Cargo:
                if (!unit.self.carryResourceType) {
                    return;
                }

                unit.self.order = (unit.isCarryingGas() ? Order.ReturnGas : Order.ReturnMinerals);
                unit.self.isGathering = true;
                unit.self.isIdle = false;

                break;

            // RLF
            case Right_Click_Position:
                unit.self.order = Order.Move;
                unit.self.targetPositionX = command.x;
                unit.self.targetPositionY = command.y;
                unit.self.orderTargetPositionX = command.x;
                unit.self.orderTargetPositionY = command.y;
                unit.self.isMoving = true;
                unit.self.isIdle = false;
                break;

            // RLF
            case Right_Click_Unit:
                unit.self.target  = getUnitID(target);
                unit.self.isIdle = false;
                unit.self.isMoving = true;

                if (unit.getType().isWorker() && target.getType().isMineralField()) {
                    unit.self.isGathering = true;
                    unit.self.order = Order.MoveToMinerals;
                }
                else if (unit.getType().isWorker() && target.getType().isRefinery()) {
                    unit.self.isGathering = true;
                    unit.self.order = Order.MoveToGas;
                }
                else if (unit.getType().isWorker() && target.getType().getRace() == Race.Terran && target.getType().whatBuilds().getKey() == unit.getType() && !target.isCompleted()) {
                    unit.self.order = Order.ConstructingBuilding;
                    unit.self.buildUnit = getUnitID(target);
                    target.self.buildUnit = getUnitID(unit);
                    unit.self.isConstructing = true;
                    target.self.isConstructing = true;
                }
                else if (unit.getType().canAttack() && target.getPlayer() != unit.getPlayer() && !target.getType().isNeutral()) {
                    unit.self.order = Order.AttackUnit;
                }
                else if(unit.getType().canMove()) {
                 unit.self.order = Order.Follow;
                }

                break;

            // RLF
            case Set_Rally_Position:
                if (!unit.getType().canProduce())
                return;
    
                unit.self.order          = Order.RallyPointTile;
                unit.self.rallyPositionX = command.x;
                unit.self.rallyPositionY = command.y;
                unit.self.rallyUnit      = -1;
    
                break;

            // RLF
            case Set_Rally_Unit:
                if (!unit.getType().canProduce()) {
                    return;
                }
                if (!target || !target.self.exists) {
                    return;
                }
    
                unit.self.order = Order.RallyPointUnit;
                unit.self.rallyUnit = getUnitID(target);
    
                break;

            // RLF
            case Siege:
                unit.self.order = Order.Sieging;
                break;

            // RLF
            case Stop:
                unit.self.order  = Order.Stop;
                unit.self.isIdle = true;
                break;

            // With train, the game does not take the supply until RLF + 1.
            // We just pretend that it happens on RLF.
            case Train: {
                UnitType unitType = UnitType.values()[command.extra];
    
                if (!isCurrentFrame) {
                    // Happens on RLF, we don't want to duplicate this.
                    player.self.minerals -= unitType.mineralPrice();
                    player.self.gas -= unitType.gasPrice();
                }
    
                // Happens on RLF + 1, we want to pretend this happens on RLF.
                unit.self.trainingQueue[unit.self.trainingQueueCount + 1] = unitType;
                unit.self.trainingQueueCount += 1;
                player.self.supplyUsed[unitType.getRace()] += unitType.supplyRequired();
    
                // Happens on RLF or RLF + 1, doesn't matter if we do twice
                unit.self.isTraining = true;
                unit.self.isIdle = false;
                unit.self.remainingTrainTime = unitType.buildTime();
    
                if (unitType == UnitType.Terran_Nuclear_Missile) {
                    unit.self.secondaryOrder = Order.Train;
                }
                }
                break;

            // RLF
            case Unburrow:
                unit.self.order = Order.Unburrowing;
                break;

            // RLF
            case Unload:
                unit.self.order  = Order.Unload;
                unit.self.target = getUnitID(target);
                break;

            // RLF
            case Unload_All:
                if (unit.getType() == UnitType.Terran_Bunker) {
                    unit.self.order = Order.Unload;
                }
                else {
                    unit.self.order                = Order.MoveUnload;
                    unit.self.targetPositionX      = command.x;
                    unit.self.targetPositionY      = command.y;
                    unit.self.orderTargetPositionX = command.x;
                    unit.self.orderTargetPositionY = command.y;
                }

                break;

            // RLF
            case Unload_All_Position:
                unit.self.order                = Order.MoveUnload;
                unit.self.targetPositionX      = command.x;
                unit.self.targetPositionY      = command.y;
                unit.self.orderTargetPositionX = command.x;
                unit.self.orderTargetPositionY = command.y;
                break;

            // RLF
            case Unsiege:
                unit.self.order = Order.Unsieging;
                break;

            // RLF
            case Upgrade: {
                UpgradeType upgradeType = UpgradeType.values()[command.extra];

                unit.self.order = Order.Upgrade;
                unit.self.upgrade = upgradeType;
                unit.self.isIdle = false;

                final int level = unit.getPlayer().getUpgradeLevel(upgradeType);
                unit.self.remainingUpgradeTime = upgradeType.upgradeTime(level + 1);

                player.self.minerals -= upgradeType.mineralPrice(level + 1);
                player.self.gas -= upgradeType.gasPrice(level + 1);

                player.self.isUpgrading[upgradeType] = true;
                }
                break;

            // RLF
            case Use_Tech:
                if (TechType.values()[command.extra] == TechType.Stim_Packs && unit.self.hitPoints > 10) {
                    unit.self.hitPoints -= 10;
                    unit.self.stimTimer = 17;
                }
                break;

            // RLF
            case Use_Tech_Position: {
                TechType techType = TechType.values()[command.extra];

                if (!techType.targetsPosition()) {
                    return;
                }

                unit.self.order                = techType.getOrder();
                unit.self.targetPositionX      = command.x;
                unit.self.targetPositionY      = command.y;
                unit.self.orderTargetPositionX = command.x;
                unit.self.orderTargetPositionY = command.y;
                }

                break;

            // RLF
            case Use_Tech_Unit: {
                TechType techType = TechType.values()[command.extra];

                if (!techType.targetsUnit()) {
                    return;
                }

                unit.self.order = techType.getOrder();
                unit.self.orderTarget = getUnitID(target);

                final Position targetPosition    = target.getPosition();

                unit.self.targetPositionX = targetPosition.x;
                unit.self.targetPositionY = targetPosition.y;
                unit.self.orderTargetPositionX = targetPosition.x;
                unit.self.orderTargetPositionY = targetPosition.y;

                break;
                }
        }
    }
}
