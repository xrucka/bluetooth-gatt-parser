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

import org.sputnikdev.bluetooth.gattparser.FieldHolder;

/**
 * Abstract class implementing common operation for all segments that format gatt fields
 * @author Lukas Rucka
 */
abstract class AbstractNamedSegment implements Segment {
    private final String fieldName;

    protected AbstractNamedSegment(String fieldName) {
        this.fieldName = fieldName;
    }

    protected FieldHolder getField(Collection<FieldHolder> holders) {
        for (FieldHolder holder : holders) {
            if (fieldName.equalsIgnoreCase(holder.getField().getName())) {
                return holder;
            }
        }
        return null;
    }

    public abstract String format(Collection<FieldHolder> holders);
}

