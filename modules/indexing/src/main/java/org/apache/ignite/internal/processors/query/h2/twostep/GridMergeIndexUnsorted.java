/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.query.h2.twostep;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Cursor;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2RowFactory;
import org.h2.index.Cursor;
import org.h2.index.IndexType;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.IndexColumn;
import org.h2.value.Value;

/**
 * Unsorted merge index.
 */
public final class GridMergeIndexUnsorted extends GridMergeIndex {
    /** */
    private final BlockingQueue<GridResultPage> queue = new LinkedBlockingQueue<>();

    /**
     * @param ctx Context.
     * @param tbl  Table.
     * @param name Index name.
     */
    public GridMergeIndexUnsorted(GridKernalContext ctx, GridMergeTable tbl, String name) {
        super(ctx, tbl, name, IndexType.createScan(false), IndexColumn.wrap(tbl.getColumns()));
    }

    /**
     * @param ctx Context.
     * @return Dummy index instance.
     */
    public static GridMergeIndexUnsorted createDummy(GridKernalContext ctx) {
        return new GridMergeIndexUnsorted(ctx);
    }

    /**
     * @param ctx Context.
     */
    private GridMergeIndexUnsorted(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override protected void addPage0(GridResultPage page) {
        assert page.rowsInPage() > 0 || page.isLast() || page.isFail();

        queue.add(page);
    }

    /** {@inheritDoc} */
    @Override protected Cursor findAllFetched(List<Row> fetched, SearchRow first, SearchRow last) {
        // This index is unsorted: have to ignore bounds.
        return new GridH2Cursor(fetched.iterator());
    }

    /** {@inheritDoc} */
    @Override protected Cursor findInStream(SearchRow first, SearchRow last) {
        // This index is unsorted: have to ignore bounds.
        return new FetchingCursor(null, null, new Iterator<Row>() {
            /** */
            Iterator<Value[]> iter = Collections.emptyIterator();

            @Override public boolean hasNext() {
                iter = pollNextIterator(queue, iter);

                return iter.hasNext();
            }

            @Override public Row next() {
                return GridH2RowFactory.create(iter.next());
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }
}