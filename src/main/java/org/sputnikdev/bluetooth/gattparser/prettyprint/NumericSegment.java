package org.sputnikdev.bluetooth.gattparser.prettyprint;

/*-
 * #%L
 * org.sputnikdev:bluetooth-gatt-parser
 * %%
 * Copyright (C) 2017 Sputnik Dev
 * Copyright (C) 2018 Lukas Rucka
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.math.BigInteger;
import java.math.BigDecimal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Function;

import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.FieldType;
import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;

/**
 * Formatted number segment
 * @author Lukas Rucka
 */
class NumericSegment extends AbstractNamedSegment {
    private String formatString;
    private Function<FieldHolder, String> operation = null;

    private Function<FieldHolder, String> makeIdentity(FieldType type) {
        switch (type) {
            case UINT:
            case SINT:
                return new Function<FieldHolder, String>() {
                    public String apply(FieldHolder value) {
                        return String.format(formatString, value.getBigInteger(BigInteger.ZERO));
                    }
                };
            case FLOAT_IEE754:
            case FLOAT_IEE11073:
                return new Function<FieldHolder, String>() {
                    public String apply(FieldHolder value) {
                        return String.format(formatString, value.getBigDecimal(BigDecimal.ZERO));
                    }
                };
        }
        return null;
    }

    private Function<FieldHolder, String> makeDivision(FieldType type, String operand) {
        switch (type) {
            case UINT:
            case SINT:
                return new Function<FieldHolder, String>() {
                    public String apply(FieldHolder value) {
                        return String.format(formatString, value.getBigInteger(BigInteger.ZERO).divide(new BigInteger(operand, 10)));
                    }
                };
            case FLOAT_IEE754:
            case FLOAT_IEE11073:
                return new Function<FieldHolder, String>() {
                    public String apply(FieldHolder value) {
                        return String.format(formatString, value.getBigDecimal(BigDecimal.ZERO).divide(new BigDecimal(operand)));
                    }
                };
        }
        return null;
    }

    private Function<FieldHolder, String> makeModulo(FieldType type, String operand) {
        switch (type) {
            case UINT:
            case SINT:
                return new Function<FieldHolder, String>() {
                    public String apply(FieldHolder value) {
                        return String.format(formatString, value.getBigInteger(BigInteger.ZERO).mod(new BigInteger(operand, 10)));
                    }
                };
            case FLOAT_IEE754:
            case FLOAT_IEE11073:
                return new Function<FieldHolder, String>() {
                    public String apply(FieldHolder value) {
                        return String.format(formatString, value.getBigDecimal(BigDecimal.ZERO).remainder(new BigDecimal(operand)));
                    }
                };
        }
        return null;
    }

    private Function<FieldHolder, String> makeOperation(FieldType type, String operator, String operand) {
        if ("/".equals(operator)) {
            return makeDivision(type, operand);
        } else if ("%".equals(operator)) {
            return makeModulo(type, operand);
        } else {
            return makeIdentity(type);
        }
    }


    NumericSegment(String fieldName, FieldType type, String operator, String operand, Integer precision, Integer width, String prefix, String spec) throws CharacteristicFormatException {
        super(fieldName);

        this.operation = this.makeOperation(type, operator, operand);
        if (this.operation == null) {
            throw new CharacteristicFormatException("Invalid specifier " + spec);
        }
        this.formatString = "%" 
            + (prefix != null ? prefix : "") 
            + (width != null ? width.toString() : "") 
            + (precision != null ? "." + precision.toString() : "") 
            + spec;
    }

    public String format(Collection<FieldHolder> holders) {
        // string formating - use standard java format
        FieldHolder fvalue = getField(holders);
        return this.operation.apply(fvalue);
    }   
}

