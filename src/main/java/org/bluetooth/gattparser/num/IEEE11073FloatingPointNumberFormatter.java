package org.bluetooth.gattparser.num;

import java.util.BitSet;

/**
 * IEEE11073 floating point number formatter.
 * Stateless and threadsafe.
 */
public class IEEE11073FloatingPointNumberFormatter implements FloatingPointNumberFormatter {

    public static final int SFLOAT_NaN = 0x07FF;
    public static final int SFLOAT_NRes = 0x0800;
    public static final int SFLOAT_POSITIVE_INFINITY = 0x07FE;
    public static final int SFLOAT_NEGATIVE_INFINITY = 0x0802;
    public static final int SFLOAT_RESERVED = 0x0801;

    public static final int FLOAT_NaN = 0x007FFFFF;
    public static final int FLOAT_NRes = 0x00800000;
    public static final int FLOAT_POSITIVE_INFINITY = 0x007FFFFE;
    public static final int FLOAT_NEGATIVE_INFINITY = 0x00800002;
    public static final int FLOAT_RESERVED = 0x00800001;

    private static final int SFLOAT_NEGATIVE_INFINITY_SIGNED = 0xFFFFF802;
    private static final int FLOAT_NEGATIVE_INFINITY_SIGNED = 0xFF800002;

    private TwosComplementNumberFormatter twosComplementNumberFormatter = new TwosComplementNumberFormatter();

    @Override
    public Float deserializeSFloat(BitSet bits) {
        BitSet exponentBits = bits.get(12, 16);
        BitSet mantissaBits = bits.get(0, 12);
        int exponent = twosComplementNumberFormatter.deserializeInteger(exponentBits, 4, true);
        int mantissa = twosComplementNumberFormatter.deserializeInteger(mantissaBits, 12, true);
        if (exponent == 0) {
            if (mantissa == SFLOAT_NaN) {
                return Float.NaN;
            } else if (mantissa == SFLOAT_POSITIVE_INFINITY) {
                return Float.POSITIVE_INFINITY;
            } else if (mantissa == SFLOAT_NEGATIVE_INFINITY_SIGNED) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return (float) ((double) mantissa * Math.pow(10, exponent));
    }

    @Override
    public Float deserializeFloat(BitSet bits) {
        BitSet exponentBits = bits.get(24, 32);
        BitSet mantissaBits = bits.get(0, 24);
        int exponent = twosComplementNumberFormatter.deserializeInteger(exponentBits, 8, true);
        int mantissa = twosComplementNumberFormatter.deserializeInteger(mantissaBits, 24, true);
        if (exponent == 0) {
            if (mantissa == FLOAT_NaN) {
                return Float.NaN;
            } else if (mantissa == FLOAT_POSITIVE_INFINITY) {
                return Float.POSITIVE_INFINITY;
            } else if (mantissa == FLOAT_NEGATIVE_INFINITY_SIGNED) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return (float) ((double) mantissa * Math.pow(10, exponent));
    }

    @Override
    public Double deserializeDouble(BitSet bits) {
        throw new IllegalStateException("Operation not supported");
    }



    @Override
    public BitSet serializeSFloat(Float number) {
        return null;
    }

    @Override
    public BitSet serializeFloat(Float number) {
        return null;
    }

    @Override
    public BitSet serializeDouble(Double number) {
        return null;
    }

}
