/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.cypher.compiler.builders.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.cypher.compiler.CypherStatementBuilder;
import org.neo4j.ogm.model.Edge;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.request.OptimisticLockingConfig;
import org.neo4j.ogm.request.Statement;
import org.neo4j.ogm.request.StatementFactory;

/**
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
public class DeletedRelationshipEntityStatementBuilder extends BaseBuilder implements CypherStatementBuilder {

    private final StatementFactory statementFactory;

    private final Set<Edge> deletedEdges;

    public DeletedRelationshipEntityStatementBuilder(Set<Edge> deletedEdges, StatementFactory statementFactory) {
        this.deletedEdges = deletedEdges;
        this.statementFactory = statementFactory;
    }

    @Override
    public Statement build() {

        final Map<String, Object> parameters = new HashMap<>();
        final StringBuilder queryBuilder = new StringBuilder();

        if (deletedEdges != null && deletedEdges.size() > 0) {
            Edge firstEdge = deletedEdges.iterator().next();

            queryBuilder.append("UNWIND {rows} AS row MATCH ()-[r]-() WHERE ID(r) = row.relId ");

            if (firstEdge.hasVersionProperty()) {
                appendVersionPropertyCheck(queryBuilder, firstEdge, "r");
            }
            queryBuilder.append("DELETE r RETURN ID(r) as ref, ID(r) as id, {type} as type");

            List<Map> rows = new ArrayList<>();
            for (Edge edge : deletedEdges) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("relId", edge.getId());
                if (edge.hasVersionProperty()) {
                    Property version = edge.getVersion();
                    rowMap.put((String) version.getKey(), version.getValue());
                }
                rows.add(rowMap);
            }
            parameters.put("rows", rows);
            parameters.put("type", "rel");

            if (firstEdge.hasVersionProperty()) {
                OptimisticLockingConfig olConfig = new OptimisticLockingConfig(rows.size(),
                    new String[] { firstEdge.getType() }, firstEdge.getVersion().getKey());
                return statementFactory.statement(queryBuilder.toString(), parameters, olConfig);
            }
        }

        return statementFactory.statement(queryBuilder.toString(), parameters);
    }
}
