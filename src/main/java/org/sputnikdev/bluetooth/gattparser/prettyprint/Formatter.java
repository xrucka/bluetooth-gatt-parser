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

import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;

/**
 * Pretty-print formater for bluetooth characteristic (interface)
 *
 * @author Lukas Rucka
 */
public interface Formatter {
    /**
     * Return pretty-printed string of characteristic, based upon <PrettyPrint> tag in it's spec.
     * @param fieldHolders a collection of value-populated field holders
     * @return String representation of the characteristic value
     * @throws CharacteristicFormatException if the provided characteristic does not contain PrettyPrint instructions
     */
    String format(Collection<FieldHolder> fieldHolders) throws CharacteristicFormatException;

    /**
     * Break the pretty-printed string back to individual fields
     * @param prettyPrinted pretty-printed string of values
     * @return Collection of parsed fields (holders)
     * @throws CharacteristicFormatException if the provided characteristic does not contain PrettyPrint instructions or parse error occured
     */
    Collection<FieldHolder> decompose(String prettyPrinted) throws CharacteristicFormatException;
}
