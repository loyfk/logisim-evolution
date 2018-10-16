package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;

public class Relay extends RelayBase {

    public Relay() {
        super("Relay", Strings.getter("relayComponent"));
    }

    @Override
    protected int getLatchStatus(InstanceState state, boolean update) {
        Object contactsVal = state.getAttributeValue(ATTR_CONTACTS);

        Value coil = state.getPortValue(COIL);

        int atRest = contactsVal == CONTACTS_OPEN ? LATCH_OPEN : LATCH_CLOSED;
        int active = contactsVal == CONTACTS_OPEN ? LATCH_CLOSED : LATCH_OPEN;

        int latch = atRest;

        if (coil.isFullyDefined() && coil == Value.TRUE) {
            latch = active;
        }

        return latch;
    }

    @Override
    public void propagate(InstanceState state) {
        int latch = getLatchStatus(state, true);
        propagateOutputs(state, latch);
    }


}
