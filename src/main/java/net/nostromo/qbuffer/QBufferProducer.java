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

public class QBufferProducer<E> {

    private final int capacity;
    private final int batchSize;
    private final int mask;
    private final E[] data;
    private final AtomicLong head;
    private final AtomicLong tail;

    private long adds;
    private long addsCapacity;
    private long addsLastCommit;

    protected QBufferProducer(final int capacity, final int batchSize, final E[] data, final AtomicLong head,
                              final AtomicLong tail) {
        this.capacity = capacity;
        this.batchSize = batchSize;
        this.mask = capacity - 1;
        this.data = data;
        this.head = head;
        this.tail = tail;
    }

    public long begin() {
        // do we need to calculate a new addsCapacity?
        if (addsCapacity == 0) addsCapacity = capacity - (adds - head.get());

        // return addsCapacity, but not greater than batchSize
        return (batchSize < addsCapacity) ? batchSize : addsCapacity;
    }

    public void add(final E e) {
        data[(int) (adds++ & mask)] = e;
    }

    public long lazySetMixCommit() {
        return commit(QBuffer.CommitMode.LAZY_SET_MIX);
    }

    public long setCommit() {
        // have we already set() committed the current adds?
        if (addsLastCommit == adds) return 0;
        addsLastCommit = adds;
        return commit(QBuffer.CommitMode.SET);
    }

    public long lazySetCommit() {
        return commit(QBuffer.CommitMode.LAZY_SET);
    }

    private long commit(final QBuffer.CommitMode mode) {
        final long addCount = adds - tail.get();
        addsCapacity -= addCount;

        switch (mode) {
            case LAZY_SET_MIX:
                // if we've used up the current addsCapacity then set(), otherwise lazySet()
                if (addsCapacity == 0) tail.set(adds);
                else tail.lazySet(adds);
                break;
            case SET:
                // just set()
                tail.set(adds);
                break;
            case LAZY_SET:
                // just lazySet()
                tail.lazySet(adds);
                break;
        }

        return addCount;
    }
}
