package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class RelayLatching extends RelayBase {

    public static RelayLatching FACTORY = new RelayLatching();

    public RelayLatching() {
        super("RelayLatching", Strings.getter("relayLatchingComponent"));
    }

    @Override
    protected int getLatchStatus(InstanceState state, boolean update) {
        Object contactsVal = state.getAttributeValue(ATTR_CONTACTS);

        int atRest = contactsVal == CONTACTS_OPEN ? LATCH_OPEN : LATCH_CLOSED;

        StateData data = (StateData) state.getData();
        if (data == null) {
            // changed = true;
            data = new StateData(atRest);
            state.setData(data);
        }

        Value coil = state.getPortValue(COIL);
        
        if (coil.isFullyDefined() && update && data.updateClock(coil)) {
            data.flip();
        }

        return data.latchStatus;
    }

    @Override
    public void propagate(InstanceState state) {
        int latch = getLatchStatus(state, true);
        propagateOutputs(state, latch);
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
