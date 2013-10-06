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

public abstract class QParticipant<E> {

    // these first 3 vars are used by both the producer and consumer threads
    protected final E[] data;
    protected final AtomicLong head;
    protected final AtomicLong tail;

    // the remaining vars are used only by either a producer or a consumer thread
    protected final int batchSize;
    protected final int mask;

    protected long ops;
    protected long opsCapacity;

    public QParticipant(final E[] data, final AtomicLong head, final AtomicLong tail, final int batchSize) {
        this.data = data;
        this.head = head;
        this.tail = tail;
        this.batchSize = batchSize;
        mask = data.length - 1;
    }

    abstract long calcOpsCapacity();

    public long begin() {
        // do we need to calculate a new opsCapacity?
        if (opsCapacity <= 0) opsCapacity = calcOpsCapacity();
        // return opsCapacity, but ensure it's not greater than batchSize
        return (batchSize < opsCapacity) ? batchSize : opsCapacity;
    }

    public long setCommit() {
        return commit(CommitMode.SET);
    }

    public long lazySetCommit() {
        return commit(CommitMode.LAZY_SET);
    }

    public long lazySetMixCommit() {
        return commit(CommitMode.LAZY_SET_MIX);
    }

    private long commit(final CommitMode mode) {
        final long opCount = ops - tail.get();
        opsCapacity -= opCount;

        switch (mode) {
            case LAZY_SET_MIX:
                // if we've used up the current opsCapacity then set(), otherwise lazySet(),
                // this logic seemed to perform better that just lazySet()
                if (opsCapacity <= 0) tail.set(ops);
                else tail.lazySet(ops);
                break;
            case SET:
                // just set()
                tail.set(ops);
                break;
            case LAZY_SET:
                // just lazySet()
                tail.lazySet(ops);
                break;
        }

        return opCount;
    }

    public int capacity() {
        return data.length;
    }

    public int batchSize() {
        return batchSize;
    }
}
