/*
 * Copyright (c) 2013 Mark D. Horton
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABIL-
 * ITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nostromo.qbuffer;

import java.util.concurrent.atomic.AtomicLong;

public class QBufferConsumer<E> {

    // these 5 final fields are shared between producer and consumer
    private final int batchSize;
    private final int mask;
    private final E[] data;
    private final AtomicLong head;
    private final AtomicLong tail;

    private long removes;
    private long removesCapacity;
    private long removesLastCommit;

    protected QBufferConsumer(final int capacity, final int batchSize, final E[] data, final AtomicLong head,
                              final AtomicLong tail) {
        this.batchSize = batchSize;
        this.mask = capacity - 1;
        this.data = data;
        this.head = head;
        this.tail = tail;
    }

    public long begin() {
        // do we need to calculate a new removesCapacity?
        if (removesCapacity == 0) removesCapacity = tail.get() - removes;

        // return removesCapacity, but not greater than batchSize
        return (batchSize < removesCapacity) ? batchSize : removesCapacity;
    }

    public E remove() {
        return data[(int) (removes++ & mask)];
    }

    public long lazySetMixCommit() {
        return commit(QBuffer.CommitMode.LAZY_SET_MIX);
    }

    public long setCommit() {
        // have we already set() committed the current removes?
        if (removesLastCommit == removes) return 0;
        removesLastCommit = removes;
        return commit(QBuffer.CommitMode.SET);
    }

    public long lazySetCommit() {
        return commit(QBuffer.CommitMode.LAZY_SET);
    }

    private long commit(final QBuffer.CommitMode mode) {
        final long addCount = removes - head.get();
        // decrement the removesCapacity
        removesCapacity -= addCount;

        switch (mode) {
            case LAZY_SET_MIX:
                // if we've used up the current removesCapacity then set(), otherwise lazySet()
                if (removesCapacity == 0) head.set(removes);
                else head.lazySet(removes);
                break;
            case SET:
                // just set()
                head.set(removes);
                break;
            case LAZY_SET:
                // just lazySet()
                head.lazySet(removes);
                break;
        }

        return addCount;
    }
}
