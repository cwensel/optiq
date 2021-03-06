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
package org.eigenbase.rel;

import java.util.List;

import org.eigenbase.rel.metadata.RelMetadataQuery;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelTraitSet;

/**
 * Abstract base class for implementations of
 * {@link MinusRel}.
 *
 * @author jhyde
 */
public abstract class MinusRelBase extends SetOpRel {
    public MinusRelBase(
        RelOptCluster cluster,
        RelTraitSet traits,
        List<RelNode> inputs,
        boolean all)
    {
        super(cluster, traits, inputs, all);
    }

    // implement RelNode
    public double getRows()
    {
        // REVIEW jvs 30-May-2005:  I just pulled this out of a hat.
        double dRows = RelMetadataQuery.getRowCount(inputs.get(0));
        for (int i = 1; i < inputs.size(); i++) {
            dRows -= 0.5 * RelMetadataQuery.getRowCount(inputs.get(i));
        }
        if (dRows < 0) {
            dRows = 0;
        }
        return dRows;
    }
}

// End MinusRelBase.java
