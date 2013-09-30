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

public class QBufferConsumer<E> extends QBufferParticipant<E> {

    public QBufferConsumer(final int capacity, final int batchSize, final E[] data, final AtomicLong head,
                    final AtomicLong tail) {
        super(capacity, batchSize, data, tail, head); // tail and head are flipped for consumer
    }

    // yen is the tail for the consumer
    long calcOpsCapacity() {
        return yen.get() - ops;
    }

    public E remove() {
        return data[(int) (ops++ & mask)];
    }
}
