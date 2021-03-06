/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.java;

import org.eigenbase.reltype.RelDataType;
import org.eigenbase.reltype.RelDataTypeFactory;

import java.lang.reflect.Type;

/**
 * Type factory that can register Java classes as record types.
 *
 * @author jhyde
 */
public interface JavaTypeFactory extends RelDataTypeFactory {
    /**
     * Creates a record type based upon the public fields of a Java class.
     *
     * @param clazz Java class
     * @return Record type that remembers its Java class
     */
    RelDataType createStructType(Class clazz);


    /**
     * Creates a type, deducing whether a record, scalar or primitive type
     * is needed.
     *
     * @param type Java type, such as a {@link Class}
     * @return Record or scalar type
     */
    RelDataType createType(Type type);

    Type getJavaClass(RelDataType type);
}

// End JavaTypeFactory.java
