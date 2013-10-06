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

public class QConsumer<E> extends QParticipant<E> {

    public QConsumer(final E[] data, final AtomicLong tail, final AtomicLong head, final int batchSize) {
        super(data, tail, head, batchSize);
    }

    // head is the queue tail for the consumer
    long calcOpsCapacity() {
        return head.get() - ops;
    }
}
