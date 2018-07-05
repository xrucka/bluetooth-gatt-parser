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

import java.util.Collection;
import java.util.LinkedHashMap;

import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;

/**
 * Formatted string segment
 * @author Lukas Rucka
 */
class StringSegment extends AbstractNamedSegment {
    private String formatString;

    StringSegment(String fieldName, Integer precision, Integer width, String prefix) {
        super(fieldName);
        this.formatString = "%" 
            + (prefix != null ? prefix : "") 
            + (width != null ? width.toString() : "") 
            + (precision != null ? "." + precision.toString() : "") 
            + "s";
    }

    public String format(Collection<FieldHolder> holders) {
        // string formating - use standard java format
        FieldHolder fvalue = getField(holders);
        return String.format(formatString, fvalue.getString());
    }   
}

