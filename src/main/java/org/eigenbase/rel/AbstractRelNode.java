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

import java.io.*;

import java.util.*;
import java.util.logging.*;

import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.*;
import org.eigenbase.trace.*;
import org.eigenbase.util.*;


/**
 * Base class for every relational expression ({@link RelNode}).
 */
public abstract class AbstractRelNode
    implements RelNode
{
    //~ Static fields/initializers ---------------------------------------------

    // TODO jvs 10-Oct-2003:  Make this thread safe.  Either synchronize, or
    // keep this per-VolcanoPlanner.

    /**
     * generator for {@link #id} values
     */
    static int nextId = 0;
    private static final Logger tracer = EigenbaseTrace.getPlannerTracer();

    //~ Instance fields --------------------------------------------------------

    /**
     * Description, consists of id plus digest.
     */
    private String desc;

    /**
     * Cached type of this relational expression.
     */
    protected RelDataType rowType;

    /**
     * A short description of this relational expression's type, inputs, and
     * other properties. The string uniquely identifies the node; another node
     * is equivalent if and only if it has the same value. Computed by {@link
     * #computeDigest}, assigned by {@link #onRegister}, returned by {@link
     * #getDigest()}.
     *
     * @see #desc
     */
    protected String digest;

    private final RelOptCluster cluster;

    /**
     * unique id of this object -- for debugging
     */
    protected int id;

    /**
     * The variable by which to refer to rows from this relational expression,
     * as correlating expressions; null if this expression is not correlated on.
     */
    private String correlVariable;

    /**
     * The RelTraitSet that describes the traits of this RelNode.
     */
    protected RelTraitSet traitSet;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates an <code>AbstractRelNode</code>.
     */
    public AbstractRelNode(RelOptCluster cluster, RelTraitSet traitSet)
    {
        super();
        assert (cluster != null);
        this.cluster = cluster;
        this.traitSet = traitSet;
        this.id = nextId++;
        this.digest = getRelTypeName() + "#" + id;
        this.desc = digest;
        if (tracer.isLoggable(Level.FINEST)) {
            tracer.finest("new " + digest);
        }
    }

    //~ Methods ----------------------------------------------------------------

    public RelNode clone()
    {
        return copy(
            getTraitSet(),
            getInputs());
    }

    public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
        // Note that empty set equals empty set, so relational expressions
        // with zero inputs do not generally need to implement their own copy
        // method.
        if (getInputs().equals(inputs)
            && traitSet == getTraitSet())
        {
            return this;
        }
        throw new AssertionError(
            "Relational expression should override copy. Class=[" + getClass()
            + "]; traits=[" + getTraitSet() + "]; desired traits=[" + traitSet
            + "]");
    }

    protected static <T> T sole(List<T> collection)
    {
        assert collection.size() == 1;
        return collection.get(0);
    }

    public boolean isAccessTo(RelOptTable table)
    {
        return getTable() == table;
    }

    public RexNode [] getChildExps()
    {
        return RexUtil.emptyExpressionArray;
    }

    public final RelOptCluster getCluster()
    {
        return cluster;
    }

    public final CallingConvention getConvention()
    {
        return (CallingConvention) traitSet.getTrait(
            CallingConventionTraitDef.instance);
    }

    public RelTraitSet getTraitSet()
    {
        return traitSet;
    }

    public void setCorrelVariable(String correlVariable)
    {
        this.correlVariable = correlVariable;
    }

    public String getCorrelVariable()
    {
        return correlVariable;
    }

    public boolean isDistinct()
    {
        return false;
    }

    public int getId()
    {
        return id;
    }

    public RelNode getInput(int i)
    {
        List<RelNode> inputs = getInputs();
        return inputs.get(i);
    }

    public String getOrCreateCorrelVariable()
    {
        if (correlVariable == null) {
            correlVariable = getQuery().createCorrel();
            getQuery().mapCorrel(correlVariable, this);
        }
        return correlVariable;
    }

    public final RelOptQuery getQuery()
    {
        return getCluster().getQuery();
    }

    public void register(RelOptPlanner planner)
    {
        Util.discard(planner);
    }

    public final String getRelTypeName()
    {
        String className = getClass().getName();
        int i = className.lastIndexOf("$");
        if (i >= 0) {
            return className.substring(i + 1);
        }
        i = className.lastIndexOf(".");
        if (i >= 0) {
            return className.substring(i + 1);
        }
        return className;
    }

    public boolean isValid(boolean fail)
    {
        return true;
    }

    public List<RelCollation> getCollationList()
    {
        return Collections.emptyList();
    }

    public final RelDataType getRowType()
    {
        if (rowType == null) {
            rowType = deriveRowType();
            assert rowType != null : this;
        }
        return rowType;
    }

    protected RelDataType deriveRowType()
    {
        // This method is only called if rowType is null, so you don't NEED to
        // implement it if rowType is always set.
        throw new UnsupportedOperationException();
    }

    public RelDataType getExpectedInputRowType(int ordinalInParent)
    {
        return getRowType();
    }

    public List<RelNode> getInputs()
    {
        return Collections.emptyList();
    }

    public double getRows()
    {
        return 1.0;
    }

    public Set<String> getVariablesStopped()
    {
        return Collections.emptySet();
    }

    public void collectVariablesUsed(Set<String> variableSet)
    {
        // for default case, nothing to do
    }

    public void collectVariablesSet(Set<String> variableSet)
    {
        if (correlVariable != null) {
            variableSet.add(correlVariable);
        }
    }

    public void childrenAccept(RelVisitor visitor)
    {
        List<RelNode> inputs = getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            visitor.visit(inputs.get(i), i, this);
        }
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // by default, assume cost is proportional to number of rows
        double rowCount = RelMetadataQuery.getRowCount(this);
        double bytesPerRow = 1;
        return planner.makeCost(
            rowCount,
            rowCount,
            0);
    }

    public void explain(RelOptPlanWriter pw)
    {
        pw.explain(this, Util.emptyStringArray, Util.emptyObjectArray);
    }

    public RelNode onRegister(RelOptPlanner planner)
    {
        List<RelNode> oldInputs = getInputs();
        List<RelNode> inputs = new ArrayList<RelNode>(oldInputs.size());
        for (int i = 0; i < oldInputs.size(); i++) {
            final RelNode input = oldInputs.get(i);
            RelNode e = planner.ensureRegistered(input, null);
            if (e != input) {
                // TODO: change 'equal' to 'eq', which is stronger.
                assert RelOptUtil.equal(
                    "rowtype of rel before registration",
                    input.getRowType(),
                    "rowtype of rel after registration",
                    e.getRowType(),
                    true);
            }
            inputs.add(e);
        }
        RelNode r = this;
        if (!Util.equalShallow(oldInputs, inputs)) {
            r = copy(getTraitSet(), inputs);
        }
        r.recomputeDigest();
        assert r.isValid(true);
        return r;
    }

    public String recomputeDigest()
    {
        String tempDigest = computeDigest();
        assert tempDigest != null : "post: return != null";
        String prefix = "rel#" + id + ":";

        // Substring uses the same underlying array of chars, so saves a bit
        // of memory.
        this.desc = prefix + tempDigest;
        this.digest = this.desc.substring(prefix.length());
        return this.digest;
    }

    public void registerCorrelVariable(String correlVariable)
    {
        assert (this.correlVariable == null);
        this.correlVariable = correlVariable;
        getQuery().mapCorrel(correlVariable, this);
    }

    public void replaceInput(
        int ordinalInParent,
        RelNode p)
    {
        throw Util.newInternal("replaceInput called on " + this);
    }

    public String toString()
    {
        return desc;
    }

    public final String getDescription()
    {
        return desc;
    }

    public final String getDigest()
    {
        return digest;
    }

    public RelOptTable getTable()
    {
        return null;
    }

    /**
     * Computes the digest. Does not modify this object.
     *
     * @post return != null
     */
    protected String computeDigest()
    {
        StringWriter sw = new StringWriter();
        RelOptPlanWriter pw =
            new RelOptPlanWriter(
                new PrintWriter(sw),
                SqlExplainLevel.DIGEST_ATTRIBUTES)
            {
                public void explain(
                    RelNode rel,
                    String [] terms,
                    Object [] values)
                {
                    List<RelNode> inputs = rel.getInputs();
                    RexNode [] childExps = rel.getChildExps();
                    assert terms.length
                        == (inputs.size() + childExps.length + values.length)
                        : "terms.length="
                        + terms.length
                        + " inputs.length=" + inputs.size()
                        + " childExps.length=" + childExps.length
                        + " values.length=" + values.length;
                    write(getRelTypeName());

                    for (int i = 0; i < traitSet.size(); i++) {
                        write(".");
                        write(traitSet.getTrait(i).toString());
                    }

                    write("(");
                    int j = 0;
                    for (int i = 0; i < inputs.size(); i++) {
                        if (j > 0) {
                            write(",");
                        }
                        write(terms[j++] + "=" + inputs.get(i).getDigest());
                    }
                    for (int i = 0; i < childExps.length; i++) {
                        if (j > 0) {
                            write(",");
                        }
                        RexNode childExp = childExps[i];
                        write(terms[j++] + "=" + childExp.toString());
                    }
                    for (int i = 0; i < values.length; i++) {
                        Object value = values[i];
                        if (j > 0) {
                            write(",");
                        }
                        write(terms[j++] + "=" + value);
                    }
                    write(")");
                }
            };
        explain(pw);
        pw.flush();
        return sw.toString();
    }
}

// End AbstractRelNode.java
