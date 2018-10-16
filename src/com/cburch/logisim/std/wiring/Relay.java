package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class Relay extends RelayBase {

    public Relay() {
        super("Relay", Strings.getter("relayComponent"));
    }

    @Override
    public void propagate(InstanceState state) {
        Object polesVal = state.getAttributeValue(ATTR_POLES);
        Object throwsVal = state.getAttributeValue(ATTR_THROWS);
        Object contactsVal = state.getAttributeValue(ATTR_CONTACTS);

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        Value coil = state.getPortValue(COIL);

        int atRest = contactsVal == CONTACTS_OPEN ? LATCH_OPEN : LATCH_CLOSED;
        int active = contactsVal == CONTACTS_OPEN ? LATCH_CLOSED : LATCH_OPEN;

        int latch = atRest;

        if (coil.isFullyDefined() && coil == Value.TRUE) {
            latch = active;
        }

        propagateOutputs(state, throwsVal, portsInfo, width, latch);
    }


}
