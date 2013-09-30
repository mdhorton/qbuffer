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

public class QBuffer2<E> {

    private final int capacity;
    private final int commitSize;
    private final int mask;
    private final int commitSizeMask;
    private final E[] data;

    private final AtomicLong tail = new AtomicLong();
    private final AtomicLong head = new AtomicLong();

    private long adds;
    private long removes;

    private long addCapacity;
    private long removeCapacity;

    private long lastAddsCommit;
    private long lastRemovesCommit;

    @SuppressWarnings("unchecked")
    public QBuffer2(final int capacity, final int commitSize) {
        this.capacity = nextPowerOf2(capacity);
        this.commitSize = Math.min(nextPowerOf2(commitSize), this.capacity);
        mask = this.capacity - 1;
        commitSizeMask = this.commitSize - 1;
        data = (E[]) new Object[this.capacity];
    }

    private static int nextPowerOf2(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public boolean offer(final E e) {
        if (addCapacity-- == 0) {
            if (lastAddsCommit != adds) {
                lastAddsCommit = adds;
                tail.lazySet(adds);
            }

            addCapacity = Math.min(commitSize, capacity - (adds - head.get()));
            if (addCapacity == 0) return false;
        }

        data[(int) (adds++ & mask)] = e;
//        if ((int) (adds & commitSizeMask) == 0) tail.lazySet(adds);
        return true;
    }

    public void commitOffers() {
        tail.set(adds);
    }

    public E poll() {
        if (removeCapacity-- == 0) {
            if (lastRemovesCommit != removes) {
                lastRemovesCommit = removes;
                head.lazySet(removes);
            }

            removeCapacity = Math.min(commitSize, tail.get() - removes);
            if (removeCapacity == 0) return null;
        }

        return data[(int) (removes++ & mask)];
//        final E e = data[(int) (removes++ & mask)];
//        if ((int) (removes & commitSizeMask) == 0) head.lazySet(removes);
//        return e;
    }
}
