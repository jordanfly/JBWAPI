// Original work Copyright (c) 2015, 2017, Igor Dimitrijevic
// Modified work Copyright (c) 2017-2018 OpenBW Team

//////////////////////////////////////////////////////////////////////////
//
// This file is part of the BWEM Library.
// BWEM is free software, licensed under the MIT/X11 License.
// A copy of the license is provided with the library in the LICENSE file.
// Copyright (c) 2015, 2017, Igor Dimitrijevic
//
//////////////////////////////////////////////////////////////////////////

package bwem.unit;

import bwapi.Unit;
import bwem.map.Map;

/**
 * Minerals Correspond to the units in BWAPI::getStaticNeutralUnits() for which
 * getType().isMineralField().
 */
public final class Mineral extends Resource {
    public Mineral(final Unit unit, final Map map) {
        super(unit, map);

        //        bwem_assert(Type().isMineralField());
        if (!unit.getType().isMineralField()) {
            throw new IllegalArgumentException(
                    "Unit is not a MineralPatch: " + unit.getClass().getName());
        }
    }

    @Override
    public int getInitialAmount() {
        return super.getUnit().getInitialResources();
    }

    @Override
    public int getAmount() {
        return super.getUnit().getResources();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Mineral)) {
            return false;
        } else {
            final Mineral that = (Mineral) object;
            return (this.getUnit().getID() == that.getUnit().getID());
        }
    }

    @Override
    public int hashCode() {
        return getUnit().hashCode();
    }
}
