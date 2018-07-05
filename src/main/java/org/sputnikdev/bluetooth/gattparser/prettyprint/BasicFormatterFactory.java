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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;

import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.gattparser.spec.FieldType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic printf-like formatter.
 * Accepts format strings in pattern specified by regular expression below, with fields in <>:
 * %\(&lt;field&gt;\)?(&lt;operator&gt;&lt;operand&gt;:)?&lt;flags&gt;?&gt;width&lt;?(.&lt;precision&gt;)?&lt;specifier&gt;
 *
 * Fields:
 *   Fields are specified either implicitly (first non-yet-used field) or explicitly (gatt field name)
 *   Field names must begin with letter and be enveloped in parentheses.
 * 
 * Flags:
 *   0 - Left-pads the number with zeroes (0) instead of spaces when padding is specified (see width sub-specifier).
 *
 * Specifiers:
 *   d - integer
 *   s - string
 *   
 * Operators:
 *   / - division by operand (applies to numbers)
 *   % - modulo by operand (applies to numbers, requires explicit field specification)
 * 
 * Operands:
 *   numbers in form [0-9]*[.]?[0-9]* (nonempty string)
 * 
 * Escapes:
 *   %% - prints %
 * 
 * Examples:
 *   %(starttime)/60:02d:%(starttime)%60:02d - %(endtime)/60:02d:%(endtime)%60:02d
 *     (for starttime=420 endtime=1300 prints "07:00 - 21:40")
 *
 * @author Lukas Rucka
 */
public class BasicFormatterFactory implements FormatterFactory {
    private Logger logger = LoggerFactory.getLogger(BasicFormatterFactory.class);

    private static final Pattern SPEC_PATTERN = Pattern.compile("^"
        + "%" + "(?<field>\\([a-zA-Z][^)]+\\))?"
        + "((?<operator>[/%])(?<argument>[0-9]*[.]?[0-9]):)?"
        + "(?<flags>[0])?"
        + "(?<width>[1-9][0-9]*)?"
        + "(?<precision>[.][1-9][0-9]*)?"
        + "(?<specifier>[ds])");

    private static class SegmentPlaceholder {
        SegmentPlaceholder(String name, DynamicSegmentArguments dynamic, Segment realSegment) {
            this.name = name;
            this.dynamicArguments = dynamic;
            this.realSegment = realSegment;
        }

        public String name = null;
        public Segment realSegment = null;
        public DynamicSegmentArguments dynamicArguments = null;
    };

    private static class DynamicSegmentArguments {
        public String fieldName = null;
        public String operator = null;
        public String operand = null;
        public String flags = null;
        public Integer width = null;
        public Integer precision = null;
        public String specifier = null;

        public DynamicSegmentArguments(Matcher matcher) {
            fieldName = matcher.group("field");
            operator = matcher.group("operator");
            operand = matcher.group("argument");
            flags = matcher.group("flags");
            String _width = matcher.group("width");
            width = null;
            String _precision = matcher.group("precision");
            precision = null;
            specifier = matcher.group("specifier");

            // ensure default values
            if (fieldName != null && !fieldName.isEmpty()) {
                fieldName = fieldName.substring(1, fieldName.length() - 1);
            }

            if (_width != null) {
                width = Integer.valueOf(_width);
            }
            if (_precision != null) {
                precision = Integer.valueOf(_precision);
            }

            if ((operator != null) != (operand != null)) {
                throw new CharacteristicFormatException("Invalid pretty-print specification " + matcher.group());
            }
        }
    }

    private String createStaticSegment(String leftover, List<SegmentPlaceholder> segments) {
        int minTokenLen = 0;

        if (leftover.charAt(0) == '%') {
            minTokenLen = 2;
            if (leftover.length() > 1 && leftover.charAt(1) != '%') {
                // throw parse error
            }
        }

        int nextSegmentPos = leftover.indexOf('%', minTokenLen);
        if (nextSegmentPos < 0) {
            segments.add(new SegmentPlaceholder(null, null, new TextSegment(leftover)));
            leftover = "";
        } else {
            segments.add(new SegmentPlaceholder(null, null, new TextSegment(leftover.substring(0, nextSegmentPos))));
            leftover = leftover.substring(nextSegmentPos);
        }

        return leftover;
    }

    private String createFormattedSegment(String leftover, Matcher matcher, List<SegmentPlaceholder> segments) {
        DynamicSegmentArguments dynamicArguments = new DynamicSegmentArguments(matcher);
        leftover = leftover.substring(matcher.end());
        segments.add(new SegmentPlaceholder(dynamicArguments.fieldName, dynamicArguments, null));

        return leftover;
    }

    private List<SegmentPlaceholder> patchSegmentNames(List<SegmentPlaceholder> segments, List<String> names) {
        Iterator<String> nextName = names.iterator();

        for (SegmentPlaceholder segment : segments) {
            if (segment.name != null) {
                continue;
            }

            if (segment.dynamicArguments == null) {
                // static text
                continue;
            }

            if (!nextName.hasNext()) {
                throw new CharacteristicFormatException("Cannot guess pretty-print field names, more fields are required than held");
            }

            segment.name = nextName.next();
        }
        
        return segments;
    }

    private List<Segment> completeDynamicSegments(List<SegmentPlaceholder> segments, Map<String, FieldType> types) throws CharacteristicFormatException {
        for (SegmentPlaceholder segment : segments) {
            if (segment.dynamicArguments == null) {
                // static text
                continue;
            }

            if (segment.realSegment != null) {
                continue;
            }

            FieldType type = types.get(segment.name);

            switch (segment.dynamicArguments.specifier) {
                case "s":
                    segment.realSegment = new StringSegment(
                        segment.name, 
                        segment.dynamicArguments.precision, segment.dynamicArguments.width,
                        segment.dynamicArguments.flags);
                    break;
                case "d":
                case "f":
                    segment.realSegment = new NumericSegment(
                        segment.name, type, 
                        segment.dynamicArguments.operator, segment.dynamicArguments.operand, 
                        segment.dynamicArguments.precision, segment.dynamicArguments.width, segment.dynamicArguments.flags, segment.dynamicArguments.specifier);
                    break;
                default:
                    throw new CharacteristicFormatException("Cannot guess pretty-print field names - unknown specifier");
            }
        }
        
        return segments.stream().map((s) -> s.realSegment).collect(Collectors.toList());
    }

    private List<Segment> createSegments(Characteristic characteristic, String ppstr) throws CharacteristicFormatException {
        String leftover = ppstr;
        List<SegmentPlaceholder> segments = new ArrayList<SegmentPlaceholder>();
       
        while (!leftover.isEmpty()) {
            Matcher matcher = SPEC_PATTERN.matcher(leftover);
            
            if (!matcher.find()) {
                leftover = createStaticSegment(leftover, segments);
            } else {
                leftover = createFormattedSegment(leftover, matcher, segments);
            }
        }

        Set<String> usedNames = segments.stream().map((s) -> s.name).filter((n) -> n != null).collect(Collectors.toSet());

        Map<String, FieldType> types = new HashMap();
        List<String> names = new ArrayList();

        for (Field f : characteristic.getValue().getFields()) {
            if (!usedNames.contains(f.getName())) {
                names.add(f.getName());
            }
            types.put(f.getName(), f.getFormat().getType());
        }

        patchSegmentNames(segments, names);
        return completeDynamicSegments(segments, types);
    }

    public Formatter prettyPrint(Characteristic characteristic) throws CharacteristicFormatException {
        String ppstr = characteristic.getValue().getPrettyPrint();
        if (ppstr == null) {
            return null;
            //throw new CharacteristicFormatException("Characteristic does not provide pretty print description");
        }

        List<Segment> segments = createSegments(characteristic, ppstr);

        return new Formatter() {
            public String format(Collection<FieldHolder> fieldHolders) throws CharacteristicFormatException {
                try {
                    return segments.stream().map((s) -> s.format(fieldHolders)).reduce("", (s, t) -> s+t);
                } catch (Exception e) {
                    logger.error("Error pretty-printing characteristic {}", e.getMessage());
                    return "Pretty-printing failed";
                }
            }

            public Collection<FieldHolder> decompose(String prettyPrinted) throws CharacteristicFormatException {
                throw new RuntimeException("Not implemented yet");
                //throw RuntimeException();
            }
        };
    }
}
