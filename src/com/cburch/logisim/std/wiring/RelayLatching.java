package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class RelayLatching extends RelayBase {

    public static RelayLatching FACTORY = new RelayLatching();

    public RelayLatching() {
        super("LatchingRelay", Strings.getter("relayLatchingComponent"));
    }

    @Override
    public void propagate(InstanceState state) {

        Integer polesVal = state.getAttributeValue(ATTR_POLES);
        Object throwsVal = state.getAttributeValue(ATTR_THROWS);
        Object contactsVal = state.getAttributeValue(ATTR_CONTACTS);

        int atRest = contactsVal == CONTACTS_OPEN ? LATCH_OPEN : LATCH_CLOSED;

        StateData data = (StateData) state.getData();
        if (data == null) {
            // changed = true;
            data = new StateData(atRest);
            state.setData(data);
        }

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        Value coil = state.getPortValue(COIL);

        if (coil.isFullyDefined() && data.updateClock(coil)) {
            data.flip();
        }

        propagateOutputs(state, throwsVal, portsInfo, width, data.latchStatus);
    }

    private static class StateData implements InstanceData {
        Value lastClock;
        int latchStatus;

        public StateData(int initial) {
            lastClock = Value.UNKNOWN;
            latchStatus = initial;
        }

        public void flip() {
            latchStatus = latchStatus == LATCH_OPEN ? LATCH_CLOSED : LATCH_OPEN;
        }

        public boolean updateClock(Value newClock) {
            Value oldClock = lastClock;
            lastClock = newClock;

            return oldClock == Value.FALSE && newClock == Value.TRUE;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

}
