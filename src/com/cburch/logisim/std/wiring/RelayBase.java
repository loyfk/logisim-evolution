package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

import javax.swing.*;
import java.awt.*;

public abstract class RelayBase extends InstanceFactory {

    public static final int COIL = 0;

    public static final Attribute<Integer> ATTR_POLES = Attributes.forIntegerRange(
            "poles", Strings.getter("relayPolesAttr"),
            1, 8
    );

    public static final AttributeOption THROWS_SINGLE = new AttributeOption("st", Strings.getter("relayThrowsSingle"));
    public static final AttributeOption THROWS_DOUBLE = new AttributeOption("dt", Strings.getter("relayThrowsDouble"));

    public static final Attribute<AttributeOption> ATTR_THROWS = Attributes.forOption(
            "throws", Strings.getter("relayThrowsAttr"),
            new AttributeOption[]{
                    THROWS_SINGLE,
                    THROWS_DOUBLE,
            }
    );

    public static final AttributeOption CONTACTS_OPEN = new AttributeOption("no", Strings.getter("relayContactsOpen"));
    public static final AttributeOption CONTACTS_CLOSED = new AttributeOption("nc", Strings.getter("relayContactsClosed"));

    public static final Attribute<AttributeOption> ATTR_CONTACTS = Attributes.forOption(
            "contacts", Strings.getter("relayContactsAttr"),
            new AttributeOption[]{
                    CONTACTS_OPEN,
                    CONTACTS_CLOSED,
            }
    );

    public static final int LATCH_OPEN = 1;
    public static final int LATCH_CLOSED = 2;

    private static final Icon ICON_P = Icons.getIcon("trans0.gif");

    private static final int LATCH_SIZE = 40;

    public RelayBase(String name, StringGetter displayName) {
        super(name, displayName);
        setAttributes(new Attribute[]{
                ATTR_POLES,
                ATTR_THROWS,
                ATTR_CONTACTS,
                StdAttr.FACING,
                Wiring.ATTR_GATE,
                StdAttr.WIDTH
        }, new Object[]{
                1,
                THROWS_SINGLE,
                CONTACTS_OPEN,
                Direction.SOUTH,
                Wiring.GATE_TOP_LEFT,
                BitWidth.ONE
        });

        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == StdAttr.FACING || attr == Wiring.ATTR_GATE) {
            instance.recomputeBounds();
            updatePorts(instance);
        } else if (attr == StdAttr.WIDTH) {
            updatePorts(instance);
        } else if (attr == ATTR_POLES || attr == ATTR_THROWS) {
            instance.recomputeBounds();
            updatePorts(instance);
        } else if (attr == ATTR_CONTACTS) {
            instance.fireInvalidated();
        }
    }

    private void drawInstance(InstancePainter painter, boolean isGhost) {
        Graphics2D g = (Graphics2D) painter.getGraphics();
        Location loc = painter.getLocation();

        Object polesVal = painter.getAttributeValue(ATTR_POLES);
        Object throwsVal = painter.getAttributeValue(ATTR_THROWS);

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        Direction from = painter.getAttributeValue(StdAttr.FACING);

        int degrees = Direction.SOUTH.toDegrees() - from.toDegrees();
        double radians = Math.toRadians((degrees + 360) % 360);

        g.translate(loc.getX(), loc.getY());
        g.rotate(radians);

        g.drawRect(0, 0, 40, (portsInfo.inputs - 1) * LATCH_SIZE);

        for (int n = 1; n < portsInfo.inputs - 1; n++) {
            int y = LATCH_SIZE * n;
            g.drawLine(0, y, 40, y);
        }

        g.translate(-loc.getX(), -loc.getY());

        if (!isGhost) {
            painter.drawPorts();
//            painter.drawPort(COIL, "C", Direction.SOUTH);
//            for (int in = 1; in < portsInfo.inputs; in++) {
//                painter.drawPort(in, String.format("I%d", in), Direction.EAST);
//            }
//            int offset = portsInfo.inputs;
//            for (int out = 0; out < portsInfo.outputs; out++) {
//                if (throwsVal == THROWS_DOUBLE && out % 2 == 1) {
//                    painter.drawPort(offset + out, String.format("-O%d", out - 1), Direction.WEST);
//                } else {
//                    painter.drawPort(offset + out, String.format("O%d", out), Direction.WEST);
//                }
//            }
        }
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        Object polesVal = attrs.getValue(ATTR_POLES);
        Object throwsVal = attrs.getValue(ATTR_THROWS);

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        return Bounds.create(0, 0, 40, (portsInfo.inputs - 1) * LATCH_SIZE);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        drawInstance(painter, false);
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        drawInstance(painter, true);
    }

    @Override
    public void paintIcon(InstancePainter painter) {
        ICON_P.paintIcon(painter.getDestination(), painter.getGraphics(), 2, 2);
    }

    private void updatePorts(Instance instance) {

        Object powerLoc = instance.getAttributeValue(Wiring.ATTR_GATE);
        Object polesVal = instance.getAttributeValue(ATTR_POLES);
        Object throwsVal = instance.getAttributeValue(ATTR_THROWS);

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        Port[] ports = new Port[portsInfo.total];

        if (powerLoc == Wiring.GATE_TOP_LEFT) {
            ports[COIL] = new Port(20, 0, Port.INPUT, StdAttr.WIDTH);
        } else {
            int height = (portsInfo.inputs - 1) * LATCH_SIZE;
            ports[COIL] = new Port(20, height, Port.INPUT, StdAttr.WIDTH);
        }

        int half = LATCH_SIZE / 2;

        for (int in = 1; in < portsInfo.inputs; in++) {
            ports[in] = new Port(0, in * LATCH_SIZE - half, Port.INPUT, StdAttr.WIDTH);
        }

        int offset = portsInfo.inputs;
        for (int out = 0; out < portsInfo.outputs; out++) {
            if (throwsVal == THROWS_DOUBLE) {
                int in = out / 2 + 1;
                if (out % 2 == 0) {
                    ports[offset + out] = new Port(40, (in * LATCH_SIZE - half) - 10, Port.OUTPUT, StdAttr.WIDTH);
                } else {
                    ports[offset + out] = new Port(40, (in * LATCH_SIZE - half) + 10, Port.OUTPUT, StdAttr.WIDTH);
                }
            } else {
                int in = out + 1;
                ports[offset + out] = new Port(40, in * LATCH_SIZE - half, Port.OUTPUT, StdAttr.WIDTH);
            }
        }

        instance.setPorts(ports);
    }

    protected RelayPorts getPorts(Object polesVal, Object throwsVal) {
        int numInputs = (Integer)polesVal;
        int numOutputs = (Integer)polesVal;

        if (throwsVal == THROWS_DOUBLE) {
            numOutputs *= 2;
        }

        // For the coil port
        numInputs += 1;

        return new RelayPorts(numInputs, numOutputs);
    }

    protected void propagateOutputs(InstanceState state, Object throwsVal, RelayPorts portsInfo, BitWidth width, int latch) {
        Value unknown = Value.createUnknown(width);
        int offset = portsInfo.inputs;
        for (int in = 0; in < portsInfo.inputs - 1; in++) {
            Value input = state.getPortValue(in + 1);

            if (throwsVal == THROWS_DOUBLE) {
                int out = offset + (in * 2);
                if (latch == LATCH_OPEN) {
                    state.setPort(out, unknown, 1);
                    state.setPort(out + 1, input, 1);
                } else {
                    state.setPort(out, input, 1);
                    state.setPort(out + 1, unknown, 1);
                }
            } else {
                int out = offset + in;
                if (latch == LATCH_OPEN) {
                    state.setPort(out, unknown, 1);
                } else {
                    state.setPort(out, input, 1);
                }
            }
        }
    }

    static class RelayPorts {
        final int inputs;
        final int outputs;
        final int total;

        RelayPorts(int inputs, int outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
            this.total = inputs + outputs;
        }
    }
}
