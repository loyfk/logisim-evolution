package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
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

        Object powerLoc = painter.getAttributeValue(Wiring.ATTR_GATE);
        Object polesVal = painter.getAttributeValue(ATTR_POLES);
        Object throwsVal = painter.getAttributeValue(ATTR_THROWS);

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        Direction from = painter.getAttributeValue(StdAttr.FACING);

        int degrees = Direction.SOUTH.toDegrees() - from.toDegrees();
        double radians = Math.toRadians((degrees + 360) % 360);

        g.translate(loc.getX(), loc.getY());
        g.rotate(radians);

        GraphicsUtil.switchToWidth(g, 2);

        int height = (portsInfo.inputs - 1) * LATCH_SIZE;
        int bs = powerLoc == Wiring.GATE_TOP_LEFT ? 10 : 0;
        g.drawRect(10, bs, 40, height);

        painter.getPortValue(COIL);
        int latch = getLatchStatus(painter, false);

        for (int n = 0; n < portsInfo.inputs - 1; n++) {
            int y = LATCH_SIZE * n + bs;
            if (y > 0) {
                g.drawLine(10, y, 50, y);
            }

            int y2 = y + LATCH_SIZE / 2;
            int ys = 10;
            g.drawLine(0, y2, 20, y2);
            if (throwsVal == THROWS_SINGLE) {
                g.drawLine(40, y2, 60, y2);
                if (latch == LATCH_OPEN) {
                    g.drawLine(20, y2, 40, y2 - ys);
                } else {
                    g.drawLine(20, y2, 40, y2);
                }
            } else {
                g.drawLine(40, y2 - ys, 60, y2 - ys);
                g.drawLine(40, y2 + ys, 60, y2 + ys);
                if (latch == LATCH_OPEN) {
                    g.drawLine(20, y2, 40, y2 - ys);
                } else {
                    g.drawLine(20, y2, 40, y2 + ys);
                }
            }
        }

        if (powerLoc == Wiring.GATE_TOP_LEFT) {
            g.drawLine(30, 0, 30, 10);
        } else {
            g.drawLine(30, height, 30, height + 10);
        }

        g.translate(-loc.getX(), -loc.getY());

        if (!isGhost) {
            painter.drawPorts();
        }
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        Object polesVal = attrs.getValue(ATTR_POLES);
        Object throwsVal = attrs.getValue(ATTR_THROWS);

        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        return Bounds.create(0, 0, 60, (portsInfo.inputs - 1) * LATCH_SIZE + 10);
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

        int bs = 0;
        if (powerLoc == Wiring.GATE_TOP_LEFT) {
            bs = 10;
            ports[COIL] = new Port(30, 0, Port.INPUT, StdAttr.WIDTH);
        } else {
            int height = (portsInfo.inputs - 1) * LATCH_SIZE;
            ports[COIL] = new Port(30, height + 10, Port.INPUT, StdAttr.WIDTH);
        }

        int half = LATCH_SIZE / 2;

        for (int in = 1; in < portsInfo.inputs; in++) {
            ports[in] = new Port(0, in * LATCH_SIZE - half + bs, Port.INPUT, StdAttr.WIDTH);
        }

        int offset = portsInfo.inputs;
        for (int out = 0; out < portsInfo.outputs; out++) {
            if (throwsVal == THROWS_DOUBLE) {
                int in = out / 2 + 1;
                if (out % 2 == 0) {
                    ports[offset + out] = new Port(60, (in * LATCH_SIZE - half + bs) - 10, Port.OUTPUT, StdAttr.WIDTH);
                } else {
                    ports[offset + out] = new Port(60, (in * LATCH_SIZE - half + bs) + 10, Port.OUTPUT, StdAttr.WIDTH);
                }
            } else {
                int in = out + 1;
                ports[offset + out] = new Port(60, in * LATCH_SIZE - half + bs, Port.OUTPUT, StdAttr.WIDTH);
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

    protected abstract int getLatchStatus(InstanceState state, boolean update);

    protected void propagateOutputs(InstanceState state, int latch) {
        Integer polesVal = state.getAttributeValue(ATTR_POLES);
        Object throwsVal = state.getAttributeValue(ATTR_THROWS);
        RelayPorts portsInfo = getPorts(polesVal, throwsVal);

        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
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
