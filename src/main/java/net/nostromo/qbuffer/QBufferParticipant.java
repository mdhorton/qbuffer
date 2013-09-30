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

public abstract class QBufferParticipant<E> {

    protected final int capacity;
    protected final int batchSize;
    protected final int mask;

    // these 3 items are shared by producer and consumer objects
    protected final E[] data;
    protected final AtomicLong yen;
    protected final AtomicLong yang;

    protected long ops;
    protected long opsCapacity;

    public QBufferParticipant(final int capacity, final int batchSize, final E[] data, final AtomicLong yen,
                              final AtomicLong yang) {
        this.capacity = capacity;
        this.batchSize = batchSize;
        this.mask = capacity - 1;
        this.data = data;
        this.yen = yen;
        this.yang = yang;
    }

    abstract long calcOpsCapacity();

    public long begin() {
        // do we need to calculate a new opsCapacity?
        if (opsCapacity == 0) opsCapacity = calcOpsCapacity();
        // return opsCapacity, but not greater than batchSize
        return (batchSize < opsCapacity) ? batchSize : opsCapacity;
    }

    public long setCommit() {
        return commit(QBuffer.CommitMode.SET);
    }

    public long lazySetCommit() {
        return commit(QBuffer.CommitMode.LAZY_SET);
    }

    public long lazySetMixCommit() {
        return commit(QBuffer.CommitMode.LAZY_SET_MIX);
    }

    private long commit(final QBuffer.CommitMode mode) {
        final long opCount = ops - yang.get();
        opsCapacity -= opCount;

        switch (mode) {
            case LAZY_SET_MIX:
                // if we've used up the current opsCapacity then set(), otherwise lazySet(),
                // this logic seemed to perform slightly better that just lazySet()
                if (opsCapacity == 0) yang.set(ops);
                else yang.lazySet(ops);
                break;
            case SET:
                // just set()
                yang.set(ops);
                break;
            case LAZY_SET:
                // just lazySet()
                yang.lazySet(ops);
                break;
        }

        return opCount;
    }
}
